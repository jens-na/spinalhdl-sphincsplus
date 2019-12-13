package sphincsplus.utils


import spinal.core.{BaseType, Bool, ClockDomain, Component}
import spinal.sim.{Signal, SimError, SimManager, SimManagerContext, SimThread}

import scala.collection.mutable.ArrayBuffer

/**
 * Utility object to count clock cycles in a Spinal simulation process.
 * The class looks for the first time the clock gets a high value and
 * starts counting from this point on.
 */
object SimClockCounter {

  def fork(body: => Unit): SimThread = SimManagerContext.current.manager.newThread(body)
  def sleep(cycles: Long): Unit = SimManagerContext.current.thread.sleep(cycles)
  var manager = SimManagerContext.current.manager
  var startTime, lastTime = 0L

  private def getBool(manager: SimManager, who: Bool): Bool = {
    val component = who.component
    if(who.isInput && component != null && component.parent == null){
      who
    }else {
      manager.userData.asInstanceOf[Component].pulledDataCache(who).asInstanceOf[Bool]
    }
  }

  private def getSignal(manager: SimManager, who: Bool): Signal ={
    val bt = getBool(manager, who)
    btToSignal(manager, bt)
  }

  private def btToSignal(manager: SimManager, bt: BaseType) = {
    if(bt.algoIncrementale != -1){
      SimError(s"UNACCESSIBLE SIGNAL : $bt isn't accessible during the simulation.\n- To fix it, call simPublic() on it durring the elaboration.")
    }

    manager.raw.userData.asInstanceOf[ArrayBuffer[Signal]](bt.algoInt)
  }

  def count(cd : ClockDomain, period : Long): Unit = {
    val signal = getSignal(manager, cd.clock)
    var current = manager.getLong(signal)
    var sleepValue = period / 2
    var gotStart = false
    fork {
      while(true) {
        sleep(sleepValue)
        lastTime = manager.time
        current = manager.getLong(signal)

        //println(s"Curr: ${current}, lastTime: ${lastTime}")
        if(current == 1L && !gotStart) {
          startTime = lastTime
          sleepValue = period
          gotStart = true
          //println(s"Got start at ${startTime}")
        }

      }
    }
  }

  def cycles(): Long = {
    (lastTime - startTime)/2
  }
}
