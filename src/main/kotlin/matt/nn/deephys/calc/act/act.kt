package matt.nn.deephys.calc.act

import matt.math.jmath.sigFigs
import matt.math.mathable.FloatWrapper

sealed interface Activation: Comparable<Activation> {
  val value: Float
  val formatted: String
  override fun compareTo(other: Activation) = value.compareTo(other.value)
}

@JvmInline
value class RawActivation(override val value: Float): Activation, FloatWrapper<RawActivation> {
  companion object {
	const val RAW_ACT_SYMBOL = "Y"
  }

  override val formatted get() = " $RAW_ACT_SYMBOL=${value.sigFigs(3)}"
  override fun fromFloat(d: Float): RawActivation {
	return RawActivation(d)
  }

  override val asFloat: Float
	get() = value

}

@JvmInline
value class NormalActivation(override val value: Float): Activation, FloatWrapper<NormalActivation> {
  companion object {
	const val NORMALIZED_ACT_SYMBOL = "Ŷ"
  }

  override val formatted get() = " $NORMALIZED_ACT_SYMBOL=${value.sigFigs(3)}"
  override fun plus(m: NormalActivation): NormalActivation {
	return NormalActivation(value + m.value)
  }

  override fun fromFloat(d: Float): NormalActivation {
	return NormalActivation(d)
  }

  override val asFloat: Float
	get() = value

  override fun div(n: Number): NormalActivation {
	return NormalActivation(value/n.toFloat())
  }
}