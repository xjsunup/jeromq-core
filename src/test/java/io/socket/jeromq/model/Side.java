package io.socket.jeromq.model;

import com.baidu.bjf.remoting.protobuf.EnumReadable;
public enum Side implements EnumReadable {

   UNKNOWN(0),

   BUY(1),

   SELL(2);

   private final int value;

   Side(int value) { this.value = value;  }

   public int value() { return value; }

}
