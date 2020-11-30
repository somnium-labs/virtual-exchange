# 가상거래소 OPEN API
- 암호화폐 거래소의 OPEN API 테스트 환경을 제공합니다.
- 대부분의 거래소와 비슷한 형태로 제작 되었습니다.
- FIX 프로토콜은 현재 제공되지 않습니다.

## Get Started
- 먼저 사용할 데이터베이스를 생성합니다.
- resource/application.conf 에서 database 설정을 변경 할 수 있습니다. (default: mock)
- 처음 실행하면 balance, members, orders, transactions 테이블이 생성 됩니다.
- 회원 가입/탈퇴 API는 별도로 존재하지 않으므로 members 테이블에 수동으로 계정을 생성 합니다.
- 관리자 이름은 `admin` 이여야 합니다.
- `/api/v2/deposit` API를 사용해서 자산을 증가 시킨 후에 사용 합니다.



## REST API
### General API Information
- 모든 엔드 포인트는 JSON 객체 또는 배열을 반환합니다.
- 모든 시간 및 타임 스탬프 관련 필드는 **milliseconds** 단위 입니다.

### Endpoint security
- PRIVATE API 호출은 `X-API-KEY` 헤더를 포함해야 합니다.


### PUBLIC API
#### Order book
Get an order book.
```
GET /api/v2/orderbook
```
Parameters:

| Name | Type | Mandatory | 
|:-------|:-------|:-------|
|   pair    | STRING      |   YES    |

Response:
```
{
    "lastUpdatedId": 1603937171353,
    "asks": [
        {
            "price": 8690.2900,
            "amount": 1.67515100
        }
    ],
    "bids": [
        {
            "price": 8660.2200,
            "amount": 0.02436300
        }
    ]
}
```

### PRIVATE API
#### New Order
Send in a new order.
```
POST /api/v2/order
```

Parameters:

| Name | Type | Mandatory | Description |
|:-------|:-------|:-------|:-------|
|   pair    | STRING      |   YES    | |
|   side    | ENUM      |   YES    |  `BUY` `SELL`|
|   type    | ENUM      |   YES    | `MARKET` `LIMIT`|
|   amount    | DECIMAL      |   YES    | |
|   price    | DECIMAL      |   NO    |
|   clientOrderId    | STRING      |   NO    | |
|   timeInForce    | ENUM      |   NO    | `GTC` `IOC` `FOK`|

Response:
```
{
    "order": {
        "pair": "BTC-USDT",
        "memberId": 3,
        "clientOrderId": "1603947165773",
        "orderId": 1603947173716,
        "price": 4000,
        "amount": 1,
        "remainAmount": 0,
        "status": "FILLED",
        "type": "LIMIT",
        "side": "SELL",
        "timeInForce": "GTC",
        "openedTime": 1603947185653,
        "canceledTime": null,
        "lastTradeTime": null
    },
    "transactions": [
        {
            "pair": "BTC-USDT",
            "memberId": 3,
            "tradeId": 1603947173717,
            "orderId": 1603947173716,
            "clientOrderId": "1603947165773",
            "relatedOrderId": 1603947173715,
            "executedTime": 1603947185670,
            "price": 4000,
            "amount": 1,
            "side": "SELL",
            "fee": 0,
            "feeCurrency": "USDT",
            "liquidity": "TAKER"
        }
    ]
}
```
#### Cancel Order
Cancel an active order.
```
DELETE /api/v2/order
```

Parameters:

| Name | Type | Mandatory | Description |
|:-------|:-------|:-------|:-------|
|   orderId    | LONG      |   NO    | Either orderId or clientOrderId must be sent.|
|   clientOrderId    | STRING      |   NO    | Either orderId or clientOrderId must be sent. |

Response:
```
{
    "pair": "BTC-USDT",
    "memberId": 3,
    "clientOrderId": "1603947737510",
    "orderId": 1603947173719,
    "price": 4000,
    "amount": 1,
    "remainAmount": 1,
    "status": "CANCELED",
    "type": "LIMIT",
    "side": "BUY",
    "timeInForce": "GTC",
    "openedTime": 1603947757373,
    "canceledTime": 1603947765063,
    "lastTradeTime": null
}
```

#### Query Order
Check an order's status.
```
GET /api/v2/queryOrder
```

Parameters:

| Name | Type | Mandatory | Description |
|:-------|:-------|:-------|:-------|
|   orderId    | LONG      |   NO    | Either orderId or clientOrderId must be sent.|
|   clientOrderId    | STRING      |   NO    | Either orderId or clientOrderId must be sent. |

Response:
```
{
    "pair": "BTC-USDT",
    "memberId": 3,
    "clientOrderId": "1603947816463",
    "orderId": 1603947173720,
    "price": 4000.00000000,
    "amount": 1.00000000,
    "remainAmount": 1.00000000,
    "status": "NEW",
    "type": "LIMIT",
    "side": "BUY",
    "timeInForce": "GTC",
    "openedTime": 1603947836334,
    "canceledTime": null,
    "lastTradeTime": null
}
```

#### Current open orders
Get all open orders.
```
GET /api/v2/openOrders
```

Parameters:

| Name | Type | Mandatory | Description |
|:-------|:-------|:-------|:-------|
|   pair    | STRING      |   NO    | If pair is set, it will get all orders on a pair.


Response:
```
[
    {
        "pair": "BTC-USDT",
        "memberId": 3,
        "clientOrderId": "1603954218108",
        "orderId": 1603954214957,
        "price": 4000,
        "amount": 1,
        "remainAmount": 1,
        "status": "NEW",
        "type": "LIMIT",
        "side": "BUY",
        "timeInForce": "GTC",
        "openedTime": 1603954238125,
        "canceledTime": null,
        "lastTradeTime": null
    }
]
```

#### Account Balance
Get all balances in the current account.
```
GET /api/v2/balances
```

Parameters:

| Name | Type | Mandatory | Description |
|:-------|:-------|:-------|:-------|
|   asset    | STRING      |   NO    | If asset is set, it will get a balance on the asset.


Response:
```
[
    {
        "asset": "BTC",
        "amount": 100.97563700,
        "locked": 0.00000000,
        "available": 100.97563700
    }
]
```

#### Account trade list
Get trades for a specific account and pair.
```
GET /api/v2/trades
```

Parameters:

| Name | Type | Mandatory | Description |
|:-------|:-------|:-------|:-------|
|   pair    | STRING      |   YES    |
|   fromId    | STRING      |   NO    | TradeId to fetch from. Default gets most recent trades.
|   startTime    | STRING      |   NO    | 
|   endTime    | STRING      |   NO    | 
|   limit    | INT      |   NO    |  Default 500; max 1000.


Response:
```
[
    {
        "pair": "BTC-USDT",
        "memberId": 3,
        "tradeId": 1603956122419,
        "orderId": 1603956122417,
        "clientOrderId": "1603956107909",
        "relatedOrderId": 1603956122418,
        "executedTime": 1603956133230,
        "price": 4000.00000000,
        "amount": 1.00000000,
        "side": "BUY",
        "fee": 0.00000000,
        "feeCurrency": "BTC",
        "liquidity": "MAKER"
    }
]
```

#### Deposit
```
POST /api/v2/deposit
```

Parameters:

| Name | Type | Mandatory 
|:-------|:-------|:-------|
|   pair    | STRING      | YES
|   amount    | DECIMAL     | YES



Response:
```
{
    "asset": "BTC",
    "amount": 107.97563700,
    "locked": 0.00000000,
    "available": 107.97563700
}
```

#### Withdrawal
```
POST /api/v2/withdrawal
```

Parameters:

| Name | Type | Mandatory 
|:-------|:-------|:-------|
|   pair    | STRING      | YES
|   amount    | DECIMAL     | YES



Response:
```
{
    "asset": "BTC",
    "amount": 101.97563700,
    "locked": 0.00000000,
    "available": 101.97563700
}
```

### ADMIN API
#### Initialize order book
Place orders using admin account.
All existing orders will be cancelled.
```
POST /api/admin/v2/orderbook
```

Parameters:

| Name | Type | Mandatory | Description |
|:-------|:-------|:-------|:-------|
|   pair    | STRING      | YES | |
|   bids    | ARRAY     | YES | First: price Second: amount|
|   asks    | ARRAY     | YES | First: price Second: amount|

Response:
```
200 OK
```

#### Clear order book
Cancel all orders.
```
DELETE /api/admin/v2/orderbook
```

Parameters:

| Name | Type | Mandatory | Description |
|:-------|:-------|:-------|:-------|
|   pair    | STRING      | NO | If pair is set, only cancel orders for that pair. |

Response:
```
200 OK
```

## WEBSOCKET API
### General API Information
- API Key를 사용하여 다음 주소로 연결 합니다.  `/ws/{API_KEY}`
- 유저 데이터는 구독하지 않아도 전송되며 마켓 데이터(오더북, 체결내역)는 페어별로 구독한 채널의 정보만 전송 됩니다.
- 

Subscribe to a stream
```
{
    "type": "subscribe",
    "channels": ["BTC-USDT@OrderBook", "BTC-USDT@Trades"]
}
```

Market Data
--------------------
#### Order book
Sent every time the order book is changed.
```
{
   "channel":"OrderBook",
   "pair":"BTC-USDT",
   "eventTime":1603959851584,
   "data":{
      "lastUpdatedId":1603959851584,
      "asks":[
         {
            "price":8690.2900,
            "amount":1.67515100
         }
      ],
      "bids":[
         {
            "price":8660.2200,
            "amount":0.02436300
         }        
      ]
   }
```

#### Market Trades
Sent with every transaction.
```
{
   "channel":"Trades",
   "pair":"BTC-USDT",
   "eventTime":1603960052445,
   "data":{
      "tradeId":1603958263038,
      "price":8659.6900,
      "amount":0.97563700,
      "buyerOrderId":1603958263021,
      "sellerOrderId":1603958263036,
      "tradeTime":1603960052409,
      "isTheBuyerTheMarketMaker":true
   }
}
```

User Data
---------------
#### Order Update
Sent whenever an order changes. (execcutionType: `NEW` `TRADE` `CANCELED`)
```
{
  "channel" : "OrderUpdate",
  "pair" : "BTC-USDT",
  "eventTime" : 1603960052437,
  "data" : {
    "orderId" : 1603958263036,
    "clientOrderId" : "1603960032436",
    "orderSide" : "SELL",
    "orderType" : "LIMIT",
    "timeInForce" : "GTC",
    "orderAmount" : 1,
    "orderPrice" : 4000,
    "executionType" : "TRADE",
    "orderStatus" : "FILLED",
    "executedAmount" : 0.97563700,
    "excutedPrice" : 8659.6900,
    "feeAmount" : 0E-12,
    "feeAsset" : "USDT",
    "transactionTime" : 1603960052409,
    "tradeId" : 1603958263038
  }
}
```


