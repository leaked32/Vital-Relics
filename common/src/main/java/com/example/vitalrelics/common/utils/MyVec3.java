package com.example.vitalrelics.common.utils;

public record MyVec3(
		double x,
		double y,
		double z) {

	public MyVec3 add(final MyVec3 other) {
		return new MyVec3(
				x + other.x,
				y + other.y,
				z + other.z
		);
	}

	public MyVec3 subtract(final MyVec3 other) {
		return new MyVec3(
				x - other.x,
				y - other.y,
				z - other.z
		);
	}

	public MyVec3 scale(final double scale) {
		return new MyVec3(
				x * scale,
				y * scale,
				z * scale
		);
	}
}
