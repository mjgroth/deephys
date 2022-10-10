package matt.nn.deephys.calc.act

import matt.math.jmath.sigFigs
import matt.math.mathable.Mathable

sealed interface Activation: Comparable<Activation> {
  val value: Float
  val formatted: String
  override fun compareTo(other: Activation) = value.compareTo(other.value)
}

@JvmInline
value class RawActivation(override val value: Float): Activation, Mathable<RawActivation> {
  companion object {
	const val RAW_ACT_SYMBOL = "Y"
  }

  override val formatted get() = " $RAW_ACT_SYMBOL=${value.sigFigs(3)}"
  override fun plus(m: RawActivation): RawActivation {
	return RawActivation(value + m.value)
  }

  override fun div(n: Number): RawActivation {
	return RawActivation(value/n.toFloat())
  }

}

@JvmInline
value class NormalActivation(override val value: Float): Activation, Mathable<NormalActivation> {
  companion object {
	const val NORMALIZED_ACT_SYMBOL = "Ŷ"
  }

  override val formatted get() = " $NORMALIZED_ACT_SYMBOL=${value.sigFigs(3)}"
  override fun plus(m: NormalActivation): NormalActivation {
	return NormalActivation(value + m.value)
  }

  override fun div(n: Number): NormalActivation {
	return NormalActivation(value/n.toFloat())
  }
}