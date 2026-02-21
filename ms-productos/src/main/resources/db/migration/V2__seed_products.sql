INSERT INTO products (name, price, description) VALUES
    ('Laptop Pro 15"', 1299.99, 'High-performance laptop with 32GB RAM'),
    ('Wireless Mouse',   29.99, 'Ergonomic wireless mouse'),
    ('USB-C Hub',        49.99, 'Multi-port USB-C hub with HDMI')
ON CONFLICT DO NOTHING;
