---
description: A guide for GitHub Copilot to craft immersive HTML-layer experiences for the Baisch LibGDX/GWT game. Scoped to what is actually reachable: index.html, styles.css, and DOM elements injected by HtmlLauncher.java.
metadata:
    author: Utkarsh Patrikar
    author_url: https://github.com/utkarsh232005
    github-path: skills/premium-frontend-ui
    github-ref: refs/heads/main
    github-repo: https://github.com/github/awesome-copilot
    github-tree-sha: 11ed1a3f37de027eaa81ed003541846491f6815c
name: premium-frontend-ui
---
# Immersive Frontend UI Craftsmanship — Baisch Edition

## Project constraint: canvas-rendered game

The game is entirely rendered by LibGDX on a `<canvas>` element. **CSS and HTML cannot style anything inside the game** (menus, cards, buttons, overlays). All UI improvements must target the HTML shell layer only:

| Reachable | Not reachable |
|---|---|
| `html/webapp/index.html` loading screen | In-game menus |
| `html/webapp/styles.css` body/letterbox | Card art, buttons, text |
| DOM elements injected by `HtmlLauncher.java` (e.g. the music `<img>`) | LibGDX scene graph |

All generated code must follow the **no-external-library rule**: no GSAP, Lenis, Framer Motion, or any CDN dependency. Use only vanilla CSS animations and vanilla JS.

---

## 1. Performance Imperative (always enforced)

- **Only animate `transform` and `opacity`** — never `width`, `height`, `top`, `left`, or `margin`. These are the only GPU-composited properties.
- **Touch guard**: wrap hover interactions in `@media (hover: hover) and (pointer: fine)`.
- **Motion accessibility**: wrap entrance animations in `@media (prefers-reduced-motion: no-preference)`. Provide a static fallback.
- **No `will-change` spam**: apply only on elements with complex continuous animation; remove after animation ends.

---

## 2. Loading Screen (`#loading-overlay`)

The loading screen in `index.html` is the only full-page HTML surface. It is shown while GWT compiles/downloads and removed when the `<canvas>` appears.

### 2.1 Staggered title entrance
Split `#loading-title` text into individual `<span>` elements. Animate each from `translateY(20px) opacity:0` to `translateY(0) opacity:1` with increasing `animation-delay` (80 ms per character). Example for "BAISCH": delays `0, 80, 160, 240, 320, 400ms`.

```css
@media (prefers-reduced-motion: no-preference) {
  #loading-title span {
    display: inline-block;
    opacity: 0;
    animation: letter-in 0.5s ease forwards;
  }
}
@keyframes letter-in {
  to { opacity: 1; transform: translateY(0); }
  from { opacity: 0; transform: translateY(20px); }
}
```

### 2.2 Atmospheric grain overlay
Add a `::after` pseudo-element on `#loading-overlay` with an inline SVG noise pattern at `mix-blend-mode: overlay` and opacity `0.03`. Adds texture without any image request.

```css
#loading-overlay::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image: url("data:image/svg+xml,..."); /* feTurbulence noise */
  mix-blend-mode: overlay;
  opacity: 0.03;
}
```

---

## 3. DOM-injected elements (`HtmlLauncher.java` JSNI)

Elements injected via JSNI are real DOM nodes and can be styled with inline JS.

### 3.1 Music button hover glow
The `<img id="baisch-music-img">` music button has no hover feedback. Add:
- CSS transition on `transform` and `box-shadow`
- `mouseenter`: scale to `1.12`, add gold `box-shadow`
- `mouseleave`: reset
- Guard with `window.matchMedia('(hover: hover) and (pointer: fine)').matches`

```js
if ($wnd.matchMedia('(hover: hover) and (pointer: fine)').matches) {
  img.style.transition = 'transform 0.15s ease, box-shadow 0.15s ease';
  img.addEventListener('mouseenter', function() {
    img.style.transform  = 'scale(1.12)';
    img.style.boxShadow  = '0 0 14px rgba(245,200,66,0.75)';
  });
  img.addEventListener('mouseleave', function() {
    img.style.transform  = '';
    img.style.boxShadow  = '';
  });
}
```

---

## 4. Letterbox background (`styles.css`)

On desktop the `body` background is visible as letterbox bars around the portrait canvas. Replace the flat `#222222` with a subtle radial gradient:

```css
body {
  background: radial-gradient(ellipse at center, #2a2a2a 0%, #111111 100%);
}
```

---

## 5. What is explicitly out of scope

Do **not** suggest or implement:
- Scroll-driven animations (the page does not scroll)
- Custom cursors (the canvas captures all pointer events)
- Navigation animations (no HTML nav exists)
- Any external JS/CSS library
- Any styling targeting elements inside the `<canvas>`
