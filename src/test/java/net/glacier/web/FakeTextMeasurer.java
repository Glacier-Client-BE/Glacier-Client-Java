package net.glacier.web;

/**
 * Deterministic text metrics for layout tests and benchmarks; no font or Minecraft dependency.
 * Glyph width is 0.5 em per character, bold text is 8% wider, and line height is 1.25 em.
 */
public final class FakeTextMeasurer implements Layout.TextMeasurer {
	public static final FakeTextMeasurer INSTANCE = new FakeTextMeasurer();

	@Override
	public float width(String text, boolean bold, float fontPx) {
		return text.length() * fontPx * 0.5f * (bold ? 1.08f : 1f);
	}

	@Override
	public float lineHeight(float fontPx) {
		return fontPx * 1.25f;
	}
}
