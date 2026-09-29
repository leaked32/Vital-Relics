package com.example.vitalrelics.common.guide;

import java.util.List;

/** Drawing primitives only. Implementations do not know about pages or markdown. */
public interface GuideCanvas {
	void text(String text, int x, int y, int argb);
	List<String> wrap(String text, int width);
	void fill(int left, int top, int right, int bottom, int argb);
	void pushClip(int left, int top, int right, int bottom);
	void popClip();
}
