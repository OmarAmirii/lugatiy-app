package nl.lugatiy.api;
import java.text.Normalizer;
import java.util.Locale;
final class Arabic {
  private Arabic() {}
  static String normalize(String text) {
    if (text == null) return "";
    return Normalizer.normalize(text, Normalizer.Form.NFC).replaceAll("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]", "").toLowerCase(Locale.ROOT).trim();
  }
}
