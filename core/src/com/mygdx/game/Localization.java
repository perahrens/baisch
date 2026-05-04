package com.mygdx.game;

import java.util.Locale;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.I18NBundle;

/**
 * Small i18n helper backed by LibGDX I18NBundle files in data/i18n/menu*.properties.
 */
public final class Localization {

  public static final String EN = "en";
  public static final String DE = "de";

  private static final String BASE_BUNDLE_PATH = "data/i18n/menu";

  private static String currentLanguage = EN;
  private static I18NBundle bundle;

  private Localization() {
  }

  public static void init(String preferredLanguage) {
    setLanguage(preferredLanguage, false);
  }

  public static String getLanguage() {
    return currentLanguage;
  }

  public static void setLanguage(String languageCode) {
    setLanguage(languageCode, true);
  }

  private static void setLanguage(String languageCode, boolean persist) {
    String normalized = normalizeLanguage(languageCode);
    currentLanguage = normalized;
    bundle = loadBundle(normalized);
    if (persist) {
      MyGdxGame.playerStorage.saveLanguage(currentLanguage);
    }
  }

  private static I18NBundle loadBundle(String languageCode) {
    try {
      FileHandle base = Gdx.files.internal(BASE_BUNDLE_PATH);
      Locale locale = new Locale(languageCode);
      return I18NBundle.createBundle(base, locale);
    } catch (Exception ex) {
      Gdx.app.log("I18n", "Falling back to English bundle: " + ex.getMessage());
      try {
        return I18NBundle.createBundle(Gdx.files.internal(BASE_BUNDLE_PATH), new Locale(EN));
      } catch (Exception ignored) {
        return null;
      }
    }
  }

  public static String tr(String key) {
    if (bundle == null) return key;
    try {
      return bundle.get(key);
    } catch (Exception ex) {
      return key;
    }
  }

  public static String tr(String key, Object... args) {
    if (bundle == null) return key;
    try {
      return bundle.format(key, args);
    } catch (Exception ex) {
      return tr(key);
    }
  }

  public static String heroName(String canonicalHeroName) {
    if (canonicalHeroName == null || canonicalHeroName.isEmpty()) return "";
    String key = "hero." + canonicalHeroName.replace(' ', '_');
    String translated = tr(key);
    return key.equals(translated) ? canonicalHeroName : translated;
  }

  public static boolean hasKey(String key) {
    String value = tr(key);
    return !key.equals(value);
  }

  private static String normalizeLanguage(String languageCode) {
    if (languageCode == null) return EN;
    String lc = languageCode.trim().toLowerCase();
    if (DE.equals(lc)) return DE;
    return EN;
  }
}