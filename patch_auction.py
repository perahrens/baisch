#!/usr/bin/env python3
"""Patch server/gameState.js to add hero auction methods."""
import os

path = os.path.join(os.path.dirname(__file__), 'server', 'gameState.js')
with open(path, 'r') as f:
    content = f.read()

# Change 1: add pendingHeroAuction to serialize()
old1 = "      pendingHeroSelection: this.pendingHeroSelection || null,\n      isTutorial: this.isTutorial || false,"
new1 = "      pendingHeroSelection: this.pendingHeroSelection || null,\n      pendingHeroAuction: this.pendingHeroAuction || null,\n      isTutorial: this.isTutorial || false,"
if old1 in content:
    content = content.replace(old1, new1, 1)
    print("Change 1 applied: pendingHeroAuction added to serialize()")
else:
    print("Change 1 SKIPPED: pattern not found")

# Change 2: add auction methods and module.exports guard before end
old2 = "}\n\nmodule.exports = GameState;"
AUCTION_METHODS = r"""

  // ── Hero auction ───────────────────────────────────────────────────────────

  /**
   * Seller initiates an auction for one of their heroes.
   * sellerIdx must equal currentPlayerIndex.
   */
  initiateHeroSale(sellerIdx, heroName, minBid) {
    const seller = this.players[sellerIdx];
    if (!seller) return false;
    if (this.currentPlayerIndex !== sellerIdx) return false;
    if (!(seller.heroes || []).includes(heroName)) return false;
    if (this.pendingHeroAuction) return false;

    const n = this.players.length;
    const biddingOrder = [];
    for (let i = 1; i < n; i++) {
      const idx = (sellerIdx + i) % n;
      if (!this.players[idx].isOut) biddingOrder.push(idx);
    }
    if (biddingOrder.length === 0) return false;

    this.pendingHeroAuction = {
      sellerIdx,
      heroName,
      minBid: Math.max(0, parseInt(minBid, 10) || 0),
      biddingOrder,
      passedPlayers: [],
      currentBidderIdx: biddingOrder[0],
      currentBid: null,
    };
    this.pushLog(`${this.pname(sellerIdx)} auctions ${heroName} (min bid: ${minBid})`, true, true);
    return true;
  }

  /** Current bidder submits a bid. Returns false if validation fails. */
  heroAuctionBid(bidderIdx, handCardIds, defCardIds) {
    const auction = this.pendingHeroAuction;
    if (!auction) return false;
    if (auction.currentBidderIdx !== bidderIdx) return false;
    if (auction.passedPlayers.includes(bidderIdx)) return false;

    const bidder = this.players[bidderIdx];
    if (!bidder) return false;

    for (const id of handCardIds) {
      if (!bidder.hand.includes(id)) return false;
    }
    for (const id of defCardIds) {
      const inDef = Object.values(bidder.defCards || {}).includes(id);
      const inTop = Object.values(bidder.topDefCards || {}).includes(id);
      if (!inDef && !inTop) return false;
    }

    const allIds = [...handCardIds, ...defCardIds];
    if (allIds.length === 0) return false;
    const totalStrength = allIds.reduce((sum, id) => sum + this.cardStrength(id), 0);

    const currentTotal = auction.currentBid ? auction.currentBid.totalStrength : 0;
    if (totalStrength <= currentTotal) return false;
    if (totalStrength < auction.minBid) return false;

    if (auction.currentBid && auction.currentBid.bidderIdx === bidderIdx) {
      this._restoreBidCards(auction.currentBid);
    }

    for (const id of handCardIds) {
      const idx = bidder.hand.indexOf(id);
      if (idx !== -1) bidder.hand.splice(idx, 1);
    }
    for (const id of defCardIds) {
      for (const slot of Object.keys(bidder.defCards || {})) {
        if (bidder.defCards[slot] === id) { delete bidder.defCards[slot]; break; }
      }
      for (const slot of Object.keys(bidder.topDefCards || {})) {
        if (bidder.topDefCards[slot] === id) { delete bidder.topDefCards[slot]; break; }
      }
    }

    auction.currentBid = { bidderIdx, handCardIds: [...handCardIds], defCardIds: [...defCardIds], totalStrength };
    this.pushLog(`${this.pname(bidderIdx)} bids ${totalStrength} for ${auction.heroName}`, true, true);
    this._advanceAuctionBidder();
    return true;
  }

  /** Current bidder passes. */
  heroAuctionPass(bidderIdx) {
    const auction = this.pendingHeroAuction;
    if (!auction) return false;
    if (auction.currentBidderIdx !== bidderIdx) return false;
    auction.passedPlayers.push(bidderIdx);
    this.pushLog(`${this.pname(bidderIdx)} passes the auction`, false, true);
    this._advanceAuctionBidder();
    return true;
  }

  /** Advance to next eligible bidder; resolve when all have had their turn. */
  _advanceAuctionBidder() {
    const auction = this.pendingHeroAuction;
    const remaining = auction.biddingOrder.filter(idx => !auction.passedPlayers.includes(idx));
    if (remaining.length === 0) { this._resolveHeroAuction(); return; }
    if (remaining.length === 1 && auction.currentBid && auction.currentBid.bidderIdx === remaining[0]) {
      this._resolveHeroAuction(); return;
    }
    const order = auction.biddingOrder;
    const curPos = order.indexOf(auction.currentBidderIdx);
    let next = null;
    for (let i = 1; i <= order.length; i++) {
      const candidate = order[(curPos + i) % order.length];
      if (!auction.passedPlayers.includes(candidate)) { next = candidate; break; }
    }
    auction.currentBidderIdx = next;
  }

  /** Restore bid cards to their owner (on cancel or outbid). */
  _restoreBidCards(bid) {
    const bidder = this.players[bid.bidderIdx];
    if (!bidder) return;
    for (const id of (bid.handCardIds || [])) bidder.hand.push(id);
    for (const id of (bid.defCardIds || [])) bidder.hand.push(id);
  }

  /** Finalize the auction: transfer hero and bid cards to seller as prey. */
  _resolveHeroAuction() {
    const auction = this.pendingHeroAuction;
    this.pendingHeroAuction = null;
    if (!auction.currentBid) {
      this.pushLog(`${auction.heroName} auction: no qualifying bid`, false, true);
      return;
    }
    const { sellerIdx, heroName, currentBid } = auction;
    this.heroAcquired(currentBid.bidderIdx, heroName);
    const seller = this.players[sellerIdx];
    if (!seller.preyCards) seller.preyCards = [];
    const allBidCards = [...(currentBid.handCardIds || []), ...(currentBid.defCardIds || [])];
    for (const id of allBidCards) seller.preyCards.push(id);
    this.pushLog(`${this.pname(currentBid.bidderIdx)} wins ${heroName} for ${currentBid.totalStrength}`, true);
  }
}

module.exports = GameState;"""

if old2 in content:
    content = content.replace(old2, AUCTION_METHODS, 1)
    print("Change 2 applied: auction methods added")
else:
    print("Change 2 SKIPPED: pattern not found in:", repr(content[-200:]))

with open(path, 'w') as f:
    f.write(content)
print(f"Done. File now has {len(content.splitlines())} lines.")
