INSERT INTO inventory (product_id, quantity, min_stock) VALUES
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 50, 5),
    ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 100, 10),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33', 30, 3)
ON CONFLICT DO NOTHING;
