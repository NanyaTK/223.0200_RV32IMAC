package gcd

import chisel3._
import chisel3.experimental.BundleLiterals._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

// chiseltestの主要な機能とScalaTestとの連携機能をインポート
import chiseltest._

/**
 * sbtからこのテストを実行するには、以下のようにします:
 * sbt 'testOnly gcd.GCDSpec'
 */
// ChiselScalatestTesterをミックスインします
class GCDSpec extends AnyFreeSpec with ChiselScalatestTester with Matchers {

  "Gcd should calculate proper greatest common denominator" in {
    // オプションを指定せず、'test'メソッドでDUTをラップします
    test(new DecoupledGcd(16)) { dut =>
      val testValues = for { x <- 0 to 10; y <- 0 to 10 } yield (x, y)

      dut.reset.poke(true.B)
      dut.clock.step(1)
      dut.reset.poke(false.B)
      dut.clock.step(1)

      var sent = 0
      var received = 0
      var cycles = 0

      // 'received'が全テストケース数に達するまでループ
      while (received < testValues.length) {
        cycles += 1
        assert(cycles < 1000, "Timeout: Simulation took too long")

        // データの送信ロジック
        if (sent < testValues.length && dut.input.ready.peek().litToBoolean) {
          dut.input.valid.poke(true.B)
          dut.input.bits.value1.poke(testValues(sent)._1.U)
          dut.input.bits.value2.poke(testValues(sent)._2.U)
          sent += 1
        } else {
          dut.input.valid.poke(false.B)
        }

        // データの受信ロジック
        if (dut.output.valid.peek().litToBoolean) {
          dut.output.ready.poke(true.B)
          val expectedGcd = BigInt(testValues(received)._1).gcd(BigInt(testValues(received)._2))
          dut.output.bits.gcd.expect(expectedGcd.U)
          received += 1
        } else {
          dut.output.ready.poke(false.B)
        }
        dut.clock.step(1)
      }
    }
  }
}