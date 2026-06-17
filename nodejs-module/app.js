const express = require('express');
const app = express();
const port = process.env.PORT || 3000;

app.get('/health', (req, res) => {
  res.json({ status: 'healthy', service: 'nodejs-orders' });
});

app.get('/orders', (req, res) => {
  const orders = [
    { id: 1, product: 'Laptop', quantity: 1 },
    { id: 2, product: 'Phone', quantity: 2 }
  ];
  res.json(orders);
});

app.listen(port, () => {
  console.log(`Node.js service running on port ${port}`);
});
