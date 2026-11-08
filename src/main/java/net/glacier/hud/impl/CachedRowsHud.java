package net.glacier.hud.impl;

import net.glacier.hud.RowsHudModule;

import java.util.List;

/** Rows rebuilt on tick and cached, so rendering never allocates. Subclasses call {@link #setRows}. */
abstract class CachedRowsHud extends RowsHudModule {
	private List<Part[]> rows;

	CachedRowsHud(String name, String desc, String placeholder) {
		super(name, desc);
		rows = single(part(placeholder, TEXT));
	}

	static List<Part[]> single(Part... row) {
		return List.<Part[]>of(row);
	}

	final void setRows(List<Part[]> rows) {
		this.rows = rows;
	}

	@Override
	protected final List<Part[]> rows() {
		return rows;
	}
}
