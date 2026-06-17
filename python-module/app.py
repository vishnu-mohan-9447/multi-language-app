from flask import Flask, jsonify
import os

app = Flask(__name__)

@app.route('/')
@app.route('/health')
def health():
    return jsonify({"status": "healthy", "service": "python-users"})

@app.route('/users')
def get_users():
    # Mock data - in real, connect to RDS
    users = [
        {"id": 1, "name": "Alice", "email": "alice@example.com"},
        {"id": 2, "name": "Bob", "email": "bob@example.com"}
    ]
    return jsonify(users)

if __name__ == '__main__':
    port = int(os.environ.get('PORT', 5000))
    app.run(host='0.0.0.0', port=port)
