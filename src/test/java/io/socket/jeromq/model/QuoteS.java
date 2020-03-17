package io.socket.jeromq.model;

import com.baidu.bjf.remoting.protobuf.FieldType;
import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class QuoteS {

   @Protobuf(fieldType=FieldType.FIXED64, order=1)
   private Long localTime;

   @Protobuf(fieldType=FieldType.INT32, order=2)
   private Integer code;

   @Protobuf(fieldType=FieldType.INT32, order=3)
   private Integer date;

   @Protobuf(fieldType=FieldType.FIXED64, order=4)
   private Long time;

   @Protobuf(fieldType=FieldType.INT32, order=5)
   private Integer status;

   @Protobuf(fieldType=FieldType.STRING, order=6)
   private String tradeFlag;

   @Protobuf(fieldType=FieldType.ENUM, order=7)
   private Side lastTrdSide;

   @Protobuf(fieldType=FieldType.INT64, order=8)
   private Long volume;

   @Protobuf(fieldType=FieldType.INT64, order=9)
   private Long amount;

   @Protobuf(fieldType=FieldType.INT64, order=10)
   private Long lastVlm;

   @Protobuf(fieldType=FieldType.INT64, order=11)
   private Long lastAmt;

   @Protobuf(fieldType=FieldType.INT32, order=12)
   private Integer numTrd;

   @Protobuf(fieldType=FieldType.FLOAT, order=13)
   private Float open;

   @Protobuf(fieldType=FieldType.FLOAT, order=14)
   private Float high;

   @Protobuf(fieldType=FieldType.FLOAT, order=15)
   private Float low;

   @Protobuf(fieldType=FieldType.FLOAT, order=16)
   private Float lastPx;

   @Protobuf(fieldType=FieldType.FLOAT, order=17)
   private Float preClose;

   @Protobuf(fieldType=FieldType.FLOAT, order=18)
   private java.util.List<Float> bid;

   @Protobuf(fieldType=FieldType.FLOAT, order=19)
   private java.util.List<Float> ask;

   @Protobuf(fieldType=FieldType.INT64, order=20)
   private java.util.List<Long> bidSize;

   @Protobuf(fieldType=FieldType.INT64, order=21)
   private java.util.List<Long> askSize;

   @Protobuf(fieldType=FieldType.INT64, order=22)
   private Long bidSizeAll;

   @Protobuf(fieldType=FieldType.INT64, order=23)
   private Long askSizeAll;

   @Protobuf(fieldType=FieldType.FLOAT, order=24)
   private Float bidVWap;

   @Protobuf(fieldType=FieldType.FLOAT, order=25)
   private Float askVWap;

   @Protobuf(fieldType=FieldType.FLOAT, order=26)
   private Float limitHigh;

   @Protobuf(fieldType=FieldType.FLOAT, order=27)
   private Float limitLow;

}
