-- V2__seed_dev_users.sql
-- Seed dev users (3 ADMIN, 3 SELLER, 5 BUYER) covering active/locked/pending/suspended statuses.
-- Password for all seeded accounts: SeedPass123!

INSERT INTO users (id, first_name, last_name, email, password_hash, profile_picture, role, enabled, email_verified, email_verified_at)
VALUES
    -- Active
    ('ff45214f-e7d5-466a-96b1-7be2c9bcc8c9', 'Admin',   'First',    'a1@rally.local',    '$2a$10$IIq.LiK5X6DF9kfefKGlgOX2oFDheGlkt1K1FBhnp0BY30VOUtsV2', 'https://png.pngtree.com/png-clipart/20230927/original/pngtree-man-avatar-image-for-profile-png-image_13001882.png', 'ADMIN',  TRUE,  TRUE,  now()),
    ('ac8e30d2-0e2e-48f6-81ab-71367fb3dfb6', 'Seller',  'First',    's1@rally.local',   '$2a$10$IIq.LiK5X6DF9kfefKGlgOX2oFDheGlkt1K1FBhnp0BY30VOUtsV2', 'https://png.pngtree.com/png-clipart/20230927/original/pngtree-man-avatar-image-for-profile-png-image_13001882.png', 'SELLER', TRUE,  TRUE,  now()),
    ('4dc618d7-290b-4eee-8cb5-6115e6085b6b', 'Buyer',   'First',    'b1@rally.local',    '$2a$10$IIq.LiK5X6DF9kfefKGlgOX2oFDheGlkt1K1FBhnp0BY30VOUtsV2', 'https://png.pngtree.com/png-clipart/20230927/original/pngtree-man-avatar-image-for-profile-png-image_13001882.png', 'BUYER',  TRUE,  TRUE,  now()),
    ('60ce018b-400f-4363-a3c3-e8a85d36dce4', 'Buyer',   'Second',    'b2@rally.local',    '$2a$10$IIq.LiK5X6DF9kfefKGlgOX2oFDheGlkt1K1FBhnp0BY30VOUtsV2', 'https://png.pngtree.com/png-clipart/20230927/original/pngtree-man-avatar-image-for-profile-png-image_13001882.png', 'BUYER',  TRUE,  TRUE,  now()),
    -- Locked
    ('c7925faf-1103-4124-81be-f5706c2c48b8', 'Admin',   'Locked',    'admin.locked@rally.local',    '$2a$10$IIq.LiK5X6DF9kfefKGlgOX2oFDheGlkt1K1FBhnp0BY30VOUtsV2', 'https://png.pngtree.com/png-clipart/20230927/original/pngtree-man-avatar-image-for-profile-png-image_13001882.png', 'ADMIN',  FALSE, TRUE,  now()),
    ('6dcb38de-9168-489e-906e-270a08a0509c', 'Seller',  'Locked',    'seller.locked@rally.local',   '$2a$10$IIq.LiK5X6DF9kfefKGlgOX2oFDheGlkt1K1FBhnp0BY30VOUtsV2', 'https://png.pngtree.com/png-clipart/20230927/original/pngtree-man-avatar-image-for-profile-png-image_13001882.png', 'SELLER', FALSE, TRUE,  now()),
    ('36312332-92ea-40b3-aa9d-79f3a70d0249', 'Buyer',   'Locked',    'buyer.locked@rally.local',    '$2a$10$IIq.LiK5X6DF9kfefKGlgOX2oFDheGlkt1K1FBhnp0BY30VOUtsV2', 'https://png.pngtree.com/png-clipart/20230927/original/pngtree-man-avatar-image-for-profile-png-image_13001882.png', 'BUYER',  FALSE, TRUE,  now()),
    -- Pending
    ('df8187ac-a315-447b-be7c-c7671ea4fc88', 'Admin',   'Pending',   'admin.pending@rally.local',   '$2a$10$IIq.LiK5X6DF9kfefKGlgOX2oFDheGlkt1K1FBhnp0BY30VOUtsV2', 'https://png.pngtree.com/png-clipart/20230927/original/pngtree-man-avatar-image-for-profile-png-image_13001882.png', 'ADMIN',  TRUE,  FALSE, NULL),
    ('946de886-93b2-45f7-b5f4-75268d0553cc', 'Seller',  'Pending',   'seller.pending@rally.local',  '$2a$10$IIq.LiK5X6DF9kfefKGlgOX2oFDheGlkt1K1FBhnp0BY30VOUtsV2', 'https://png.pngtree.com/png-clipart/20230927/original/pngtree-man-avatar-image-for-profile-png-image_13001882.png', 'SELLER', TRUE,  FALSE, NULL),
    ('fea62c6f-e473-4da7-8ebe-dda30801f453', 'Buyer',   'Pending',   'buyer.pending@rally.local',   '$2a$10$IIq.LiK5X6DF9kfefKGlgOX2oFDheGlkt1K1FBhnp0BY30VOUtsV2', 'https://png.pngtree.com/png-clipart/20230927/original/pngtree-man-avatar-image-for-profile-png-image_13001882.png', 'BUYER',  TRUE,  FALSE, NULL),
    -- Suspended
    ('4893b6bb-bda3-4489-ac1c-c249ad0abf69', 'Buyer',   'Suspended', 'buyer.suspended@rally.local', '$2a$10$IIq.LiK5X6DF9kfefKGlgOX2oFDheGlkt1K1FBhnp0BY30VOUtsV2', 'https://png.pngtree.com/png-clipart/20230927/original/pngtree-man-avatar-image-for-profile-png-image_13001882.png', 'BUYER',  FALSE, FALSE, NULL)
ON CONFLICT (email) DO NOTHING;
