# Stock Exchange API

A real-time stock price streaming application built with Spring Boot, WebSocket/STOMP, and RabbitMQ. This API simulates a stock exchange by generating and broadcasting live stock price updates to connected clients.

## 🎯 Purpose

The Stock Exchange API provides:
- **Real-time stock price updates** for 10 major tech stocks (AAPL, GOOGL, MSFT, TSLA, AMZN, NVDA, META, NFLX, AMD, INTC)
- **WebSocket/STOMP messaging** for instant price delivery to clients
- **Multi-instance scalability** using RabbitMQ as a message broker
- **Historical price data** stored in PostgreSQL (Azure Cosmos DB for PostgreSQL)
- **Horizontal scaling** support for handling millions of concurrent connections

## 🏗️ Architecture

```
┌─────────────┐         WebSocket/STOMP          ┌──────────────────┐
│   Browser   │◄──────────────────────────────►  │  Spring Boot App │
│   Clients   │     /ws or /ws-stomp             │  (Container App) │
└─────────────┘                                  └──────────────────┘
                                                          │
                                                          │ STOMP/TCP
                                                          │ Port 61613
                                                          ▼
                                                  ┌──────────────────┐
                                                  │    RabbitMQ      │
                                                  │ (Message Broker) │
                                                  └──────────────────┘
                                                          │
                                                          │ Persist
                                                          ▼
                                                  ┌──────────────────┐
                                                  │   Cosmos DB      │
                                                  │   (PostgreSQL)   │
                                                  └──────────────────┘
```

## 🚀 Entry Points

### WebSocket Endpoints

The API exposes WebSocket endpoints for real-time communication:

| Endpoint | Protocol | Description |
|----------|----------|-------------|
| `/ws` | WebSocket + SockJS | Primary endpoint with fallback support |
| `/ws-stomp` | WebSocket + STOMP | Alternative STOMP endpoint |

**Full URL Example:**
```
https://stock-exchange-api.proudsand-12345678.eastus.azurecontainerapps.io/ws
```

### REST Endpoints (Optional)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/stocks/current` | GET | Get current prices for all stocks |
| `/api/stocks/{symbol}` | GET | Get current price for specific stock |

## 📡 Using from the Browser

### Quick Start with JavaScript

```javascript
// 1. Connect to WebSocket endpoint
const socket = new SockJS('https://your-app-url.azurecontainerapps.io/ws');
const stompClient = Stomp.over(socket);

// 2. Establish connection
stompClient.connect({}, function(frame) {
    console.log('Connected:', frame);
    
    // 3. Subscribe to stock updates
    // Subscribe to ALL stocks
    stompClient.subscribe('/exchange/amq.topic/stock.*', function(message) {
        const stockPrice = JSON.parse(message.body);
        console.log('Received:', stockPrice);
        updateUI(stockPrice);
    });
    
    // Or subscribe to specific stock
    stompClient.subscribe('/exchange/amq.topic/stock.AAPL', function(message) {
        const stockPrice = JSON.parse(message.body);
        console.log('AAPL:', stockPrice);
    });
});
```

### Message Format

Each stock price update contains:

```json
{
  "id": 12345,
  "symbol": "AAPL",
  "price": 175.50,
  "change": 2.30,
  "changePercent": 1.33,
  "dayHigh": 176.80,
  "dayLow": 173.20,
  "openPrice": 174.00,
  "previousClose": 173.20,
  "volume": 5234567,
  "timestamp": "2025-12-22T14:30:00Z"
}
```

## 🎨 Demo UI - index.html

The included `index.html` file serves as a **live demonstration dashboard** for the Stock Exchange API. It showcases real-time WebSocket connectivity and provides an interactive interface for monitoring stock prices.

### Features

- **Real-time Price Display**: Shows live updates for all 10 stocks in a responsive grid layout
- **WebSocket Connection Management**: Connect/Disconnect buttons with visual status indicators
- **SockJS Fallback Support**: Automatically handles browsers that don't support native WebSocket
- **STOMP Protocol**: Uses STOMP over WebSocket for reliable message delivery
- **Visual Indicators**: Color-coded price changes (green for up, red for down)
- **Stock Details**: Displays high/low prices, volume, and last update time
- **Message Counter**: Tracks the number of real-time updates received

### Usage

1. Open `index.html` in any modern web browser
2. Replace `YOUR_CONTAINER_APP_URL_HERE` with your actual Azure Container App URL
3. Click "Connect" to establish WebSocket connection
4. Watch as stock prices update in real-time (every second)

The demo file requires no server installation and connects directly to your deployed Stock Exchange API, making it perfect for testing and demonstrations.

## 🔌 Subscription Patterns

### Subscribe to All Stocks (Recommended)
```javascript
stompClient.subscribe('/exchange/amq.topic/stock.*', callback);
```

### Subscribe to Specific Stock
```javascript
stompClient.subscribe('/exchange/amq.topic/stock.AAPL', callback);
```

### Subscribe to Multiple Stocks
```javascript
const symbols = ['AAPL', 'GOOGL', 'MSFT'];
symbols.forEach(symbol => {
    stompClient.subscribe(`/exchange/amq.topic/stock.${symbol}`, callback);
});
```

### Subscribe with Pattern Matching
```javascript
// Subscribe to stocks starting with 'A'
stompClient.subscribe('/exchange/amq.topic/stock.A*', callback);
```

## 🛠️ Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | PostgreSQL connection string | `jdbc:postgresql://localhost:5432/stockdb` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `dbadmin` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `password` |
| `RABBITMQ_STOMP_ENABLED` | Enable RabbitMQ broker | `true` |
| `RABBITMQ_HOST` | RabbitMQ hostname | `localhost` |
| `RABBITMQ_STOMP_PORT` | RabbitMQ STOMP port | `61613` |
| `RABBITMQ_USERNAME` | RabbitMQ username | `stockadmin` |
| `RABBITMQ_PASSWORD` | RabbitMQ password | `StockExchange2024!` |

### Application Properties

```yaml
stock-exchange:
  update-interval: 1000        # Price update frequency (ms)
  volatility: 0.02             # Price change volatility (2%)
  symbols: AAPL,GOOGL,MSFT,TSLA,AMZN,NVDA,META,NFLX,AMD,INTC
```

## 📦 Deployment

### Azure Container Apps

```bash
# Get your Container App URL
az containerapp show \
  --name stock-exchange-api \
  --resource-group stock-exchange-rg \
  --query "properties.configuration.ingress.fqdn" \
  --output tsv

# View logs
az containerapp logs show \
  --name stock-exchange-api \
  --resource-group stock-exchange-rg \
  --follow
```

### Scaling

The application automatically scales based on:
- **Min replicas**: 2 (always-on for high availability)
- **Max replicas**: 10 (scales up under load)
- **RabbitMQ**: Distributes messages across all instances

## 🧪 Testing

### Using Browser Console

Open browser console (F12) and run:

```javascript
const socket = new SockJS('https://your-app-url.azurecontainerapps.io/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function() {
    console.log('Connected!');
    stompClient.subscribe('/exchange/amq.topic/stock.*', function(msg) {
        console.log('Update:', JSON.parse(msg.body));
    });
});
```

### Using cURL (REST endpoints)

```bash
# Get all current prices
curl https://your-app-url.azurecontainerapps.io/api/stocks/current

# Get specific stock
curl https://your-app-url.azurecontainerapps.io/api/stocks/AAPL
```

## 📊 Monitoring

### Check RabbitMQ Management UI

```
http://rabbitmq-stomp-broker.eastus.azurecontainer.io:15672
Username: stockadmin
Password: StockExchange2024!
```

### View Active Connections

Check the "Connections" tab in RabbitMQ Management UI to see:
- Connected Spring Boot instances
- Active STOMP sessions
- Message rates and throughput

## 🔒 Security Considerations

For production deployments:
1. Enable HTTPS/WSS for WebSocket connections
2. Implement authentication/authorization
3. Use Azure Key Vault for secrets
4. Configure CORS properly for your domain
5. Enable rate limiting
6. Use Azure Application Gateway for DDoS protection

## 🤝 Dependencies

- **Spring Boot 3.x** - Application framework
- **Spring WebSocket** - WebSocket support
- **Spring STOMP** - STOMP protocol
- **RabbitMQ** - Message broker
- **PostgreSQL** - Database (Azure Cosmos DB)
- **SockJS** - WebSocket fallback (client-side)
- **STOMP.js** - STOMP client (browser)

## 📝 License

This project is for demonstration purposes.

## 🆘 Troubleshooting

### Connection Issues

1. **Can't connect from browser**
    - Verify Container App is running
    - Check ingress is enabled and external
    - Ensure `/ws` endpoint is accessible

2. **No price updates received**
    - Check RabbitMQ container is running
    - Verify RabbitMQ STOMP plugin is enabled
    - Check subscription path is correct: `/exchange/amq.topic/stock.*`

3. **"Invalid destination" errors**
    - Ensure using `/exchange/amq.topic/` prefix
    - Check RabbitMQ configuration in application.yml
    - Verify STOMP relay is properly configured

### Debugging

Enable debug logging in browser console:
```javascript
stompClient.debug = function(str) {
    console.log('STOMP:', str);
};
```

## 📧 Support

For issues or questions, check the Azure Container Apps logs:
```bash
az containerapp logs show \
  --name stock-exchange-api \
  --resource-group stock-exchange-rg \
  --follow
```