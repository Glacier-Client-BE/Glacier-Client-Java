package net.glacier.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class SliderSetting extends Setting<Double> {
	private final double min;
	private final double max;
	private final double step;

	public SliderSetting(String name, double def, double min, double max, double step) {
		super(name, def);
		this.min = min;
		this.max = max;
		this.step = step;
		this.value = snap(def);
		rememberDefault();
	}

	/** Clamps to [min, max] and snaps to the step grid. */
	private double snap(double v) {
		double c = Math.max(min, Math.min(max, v));
		if (step > 0) {
			c = Math.round((c - min) / step) * step + min;
			c = Math.max(min, Math.min(max, c));
		}
		return Math.round(c * 1_000_000.0) / 1_000_000.0;
	}

	@Override
	public void set(Double v) {
		if (v == null) return;
		value = snap(v);
	}

	public double min() {
		return min;
	}

	public double max() {
		return max;
	}

	public double step() {
		return step;
	}

	@Override
	public SliderSetting hide() {
		super.hide();
		return this;
	}

	public float f() {
		return value.floatValue();
	}

	public int i() {
		return (int) Math.round(value);
	}

	/** Position of the current value on the track, 0..1. */
	public double fraction() {
		double range = max - min;
		if (range <= 0) return 0;
		return (value - min) / range;
	}

	public void setFraction(double f) {
		double clamped = Math.max(0, Math.min(1, f));
		set(min + clamped * (max - min));
	}

	@Override
	public JsonElement save() {
		return new JsonPrimitive(value);
	}

	@Override
	public void load(JsonElement e) {
		if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
			set(e.getAsDouble());
		}
	}
}
