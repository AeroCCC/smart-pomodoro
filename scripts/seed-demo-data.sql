-- Smart PomoTodo demo seed data
-- Target schema: Spring Data JPA generated tables in this project.
-- Compatible with the default H2 dev database and the MySQL production schema.
--
-- Demo login accounts:
--   alice_demo / password123
--   bob_demo   / password123
--   chen_demo  / password123
--
-- Main table relationships:
--   users
--     -> tasks.user_id, tasks.assigned_to_id
--     -> focus_logs.user_id
--     -> teams.owner_id
--     -> team_members.user_id
--     -> push_subscriptions.user_id
--     -> ai_plan_drafts.user_id
--   teams
--     -> tasks.team_id
--     -> team_members.team_id
--   ai_plan_drafts
--     -> ai_plan_milestones.draft_id
--   ai_plan_milestones
--     -> ai_plan_tasks.milestone_id

-- Optional quick inspection before seeding.
SELECT 'users' AS table_name, COUNT(*) AS row_count FROM users;
SELECT 'tasks' AS table_name, COUNT(*) AS row_count FROM tasks;
SELECT 'focus_logs' AS table_name, COUNT(*) AS row_count FROM focus_logs;
SELECT 'teams' AS table_name, COUNT(*) AS row_count FROM teams;
SELECT 'team_members' AS table_name, COUNT(*) AS row_count FROM team_members;
SELECT 'ai_plan_drafts' AS table_name, COUNT(*) AS row_count FROM ai_plan_drafts;
SELECT 'ai_plan_milestones' AS table_name, COUNT(*) AS row_count FROM ai_plan_milestones;
SELECT 'ai_plan_tasks' AS table_name, COUNT(*) AS row_count FROM ai_plan_tasks;
SELECT 'push_subscriptions' AS table_name, COUNT(*) AS row_count FROM push_subscriptions;

-- Remove old demo rows first so this script can be re-run.
DELETE FROM ai_plan_tasks
WHERE milestone_id IN (
    SELECT id FROM ai_plan_milestones
    WHERE draft_id IN (
        SELECT id FROM ai_plan_drafts
        WHERE user_id IN (
            SELECT id FROM users
            WHERE username IN ('alice_demo', 'bob_demo', 'chen_demo')
        )
    )
);

DELETE FROM ai_plan_milestones
WHERE draft_id IN (
    SELECT id FROM ai_plan_drafts
    WHERE user_id IN (
        SELECT id FROM users
        WHERE username IN ('alice_demo', 'bob_demo', 'chen_demo')
    )
);

DELETE FROM ai_plan_drafts
WHERE user_id IN (
    SELECT id FROM users
    WHERE username IN ('alice_demo', 'bob_demo', 'chen_demo')
);

DELETE FROM push_subscriptions
WHERE user_id IN (
    SELECT id FROM users
    WHERE username IN ('alice_demo', 'bob_demo', 'chen_demo')
);

DELETE FROM focus_logs
WHERE user_id IN (
    SELECT id FROM users
    WHERE username IN ('alice_demo', 'bob_demo', 'chen_demo')
);

DELETE FROM tasks
WHERE user_id IN (
    SELECT id FROM users
    WHERE username IN ('alice_demo', 'bob_demo', 'chen_demo')
)
OR assigned_to_id IN (
    SELECT id FROM users
    WHERE username IN ('alice_demo', 'bob_demo', 'chen_demo')
)
OR team_id IN (
    SELECT id FROM teams
    WHERE invite_code = 'DEMO2026'
);

DELETE FROM team_members
WHERE user_id IN (
    SELECT id FROM users
    WHERE username IN ('alice_demo', 'bob_demo', 'chen_demo')
)
OR team_id IN (
    SELECT id FROM teams
    WHERE invite_code = 'DEMO2026'
);

DELETE FROM teams
WHERE invite_code = 'DEMO2026';

DELETE FROM users
WHERE username IN ('alice_demo', 'bob_demo', 'chen_demo');

use pomotodo

INSERT INTO users
    (username, email, password, avatar, created_at, updated_at, enabled, role)
VALUES
    ('alice_demo', 'alice.demo@smart-pomotodo.local', '$2a$10$ZPNW9fgMJ8Y4bSkVjgqjj.HcU2n2ngwv4x.aNNx/78RPA0AFkvQYe', 'https://api.dicebear.com/7.x/initials/svg?seed=Alice', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE, 'ADMIN'),
    ('bob_demo', 'bob.demo@smart-pomotodo.local', '$2a$10$ZPNW9fgMJ8Y4bSkVjgqjj.HcU2n2ngwv4x.aNNx/78RPA0AFkvQYe', 'https://api.dicebear.com/7.x/initials/svg?seed=Bob', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE, 'USER'),
    ('chen_demo', 'chen.demo@smart-pomotodo.local', '$2a$10$ZPNW9fgMJ8Y4bSkVjgqjj.HcU2n2ngwv4x.aNNx/78RPA0AFkvQYe', 'https://api.dicebear.com/7.x/initials/svg?seed=Chen', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE, 'USER');

-- Personal tasks.
INSERT INTO tasks
    (text, priority, completion_definition, estimated_pomodoros, completed, created_at, completed_at, deadline, user_id, team_id, assigned_to_id, status)
VALUES
    ('Finish graduation project database chapter', 'high', 'ER diagram, table description, and seed data screenshots are ready.', 4, FALSE, CURRENT_TIMESTAMP, NULL, TIMESTAMP '2026-05-20 18:00:00', (SELECT id FROM users WHERE username = 'alice_demo'), NULL, NULL, 'IN_PROGRESS'),
    ('Prepare weekly focus report', 'medium', 'Export focus statistics and summarize task completion.', 2, FALSE, CURRENT_TIMESTAMP, NULL, TIMESTAMP '2026-05-18 10:00:00', (SELECT id FROM users WHERE username = 'alice_demo'), NULL, NULL, 'TODO'),
    ('Review frontend task board interaction', 'medium', 'Verify create, update, complete, and delete flows.', 3, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TIMESTAMP '2026-05-14 20:00:00', (SELECT id FROM users WHERE username = 'bob_demo'), NULL, NULL, 'DONE'),
    ('Collect productivity app references', 'low', 'Five comparable products are listed with useful feature notes.', 2, FALSE, CURRENT_TIMESTAMP, NULL, TIMESTAMP '2026-05-22 22:00:00', (SELECT id FROM users WHERE username = 'chen_demo'), NULL, NULL, 'TODO');

-- Team and memberships.
INSERT INTO teams
    (name, description, invite_code, owner_id, created_at, updated_at, is_active)
VALUES
    ('Graduation Project Team', 'Demo team for Smart PomoTodo collaboration features.', 'DEMO2026', (SELECT id FROM users WHERE username = 'alice_demo'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE);

INSERT INTO team_members
    (team_id, user_id, role, joined_at, is_active)
VALUES
    ((SELECT id FROM teams WHERE invite_code = 'DEMO2026'), (SELECT id FROM users WHERE username = 'alice_demo'), 'OWNER', CURRENT_TIMESTAMP, TRUE),
    ((SELECT id FROM teams WHERE invite_code = 'DEMO2026'), (SELECT id FROM users WHERE username = 'bob_demo'), 'ADMIN', CURRENT_TIMESTAMP, TRUE),
    ((SELECT id FROM teams WHERE invite_code = 'DEMO2026'), (SELECT id FROM users WHERE username = 'chen_demo'), 'MEMBER', CURRENT_TIMESTAMP, TRUE);

-- Team tasks.
INSERT INTO tasks
    (text, priority, completion_definition, estimated_pomodoros, completed, created_at, completed_at, deadline, user_id, team_id, assigned_to_id, status)
VALUES
    ('Draw final ER diagram', 'high', 'The diagram includes users, tasks, teams, focus logs, and AI plan tables.', 3, FALSE, CURRENT_TIMESTAMP, NULL, TIMESTAMP '2026-05-17 21:00:00', (SELECT id FROM users WHERE username = 'alice_demo'), (SELECT id FROM teams WHERE invite_code = 'DEMO2026'), (SELECT id FROM users WHERE username = 'bob_demo'), 'IN_PROGRESS'),
    ('Write database seed-data explanation', 'medium', 'Explain why the demo data covers main user workflows.', 2, FALSE, CURRENT_TIMESTAMP, NULL, TIMESTAMP '2026-05-19 18:00:00', (SELECT id FROM users WHERE username = 'alice_demo'), (SELECT id FROM teams WHERE invite_code = 'DEMO2026'), (SELECT id FROM users WHERE username = 'chen_demo'), 'TODO'),
    ('Verify team task permissions', 'medium', 'Owner, admin, and member operations are tested through the API.', 3, FALSE, CURRENT_TIMESTAMP, NULL, TIMESTAMP '2026-05-21 18:00:00', (SELECT id FROM users WHERE username = 'bob_demo'), (SELECT id FROM teams WHERE invite_code = 'DEMO2026'), (SELECT id FROM users WHERE username = 'alice_demo'), 'TODO');

-- Focus logs. Duration is stored in seconds.
INSERT INTO focus_logs
    (date, duration, start_time, end_time, user_id)
VALUES
    (CURRENT_DATE, 1500, TIMESTAMP '2026-05-15 09:00:00', TIMESTAMP '2026-05-15 09:25:00', (SELECT id FROM users WHERE username = 'alice_demo')),
    (CURRENT_DATE, 1500, TIMESTAMP '2026-05-15 09:35:00', TIMESTAMP '2026-05-15 10:00:00', (SELECT id FROM users WHERE username = 'alice_demo')),
    (CURRENT_DATE, 1800, TIMESTAMP '2026-05-15 10:15:00', TIMESTAMP '2026-05-15 10:45:00', (SELECT id FROM users WHERE username = 'bob_demo')),
    (DATE '2026-05-14', 1500, TIMESTAMP '2026-05-14 20:00:00', TIMESTAMP '2026-05-14 20:25:00', (SELECT id FROM users WHERE username = 'chen_demo'));

-- AI plan draft with milestones and tasks.
INSERT INTO ai_plan_drafts
    (user_id, goal, normalized_goal, status, created_at, applied_at)
VALUES
    ((SELECT id FROM users WHERE username = 'alice_demo'), 'Prepare the database chapter for the graduation thesis', 'prepare database chapter for graduation thesis', 'GENERATED', CURRENT_TIMESTAMP, NULL);

INSERT INTO ai_plan_milestones
    (draft_id, title, summary, sort_order)
VALUES
    ((SELECT id FROM ai_plan_drafts WHERE normalized_goal = 'prepare database chapter for graduation thesis' AND user_id = (SELECT id FROM users WHERE username = 'alice_demo')), 'Understand current schema', 'Confirm tables, fields, and relationships from JPA entities.', 1),
    ((SELECT id FROM ai_plan_drafts WHERE normalized_goal = 'prepare database chapter for graduation thesis' AND user_id = (SELECT id FROM users WHERE username = 'alice_demo')), 'Prepare sample data', 'Add realistic records that cover personal and team workflows.', 2),
    ((SELECT id FROM ai_plan_drafts WHERE normalized_goal = 'prepare database chapter for graduation thesis' AND user_id = (SELECT id FROM users WHERE username = 'alice_demo')), 'Write thesis explanation', 'Turn schema and data findings into a concise written section.', 3);

INSERT INTO ai_plan_tasks
    (milestone_id, text, priority, completion_definition, estimated_pomodoros, suggested_deadline, sort_order, selected)
VALUES
    ((SELECT id FROM ai_plan_milestones WHERE title = 'Understand current schema' AND draft_id = (SELECT id FROM ai_plan_drafts WHERE normalized_goal = 'prepare database chapter for graduation thesis' AND user_id = (SELECT id FROM users WHERE username = 'alice_demo'))), 'List all entity tables and foreign keys', 'high', 'Every table and relationship is documented.', 2, TIMESTAMP '2026-05-16 18:00:00', 1, TRUE),
    ((SELECT id FROM ai_plan_milestones WHERE title = 'Prepare sample data' AND draft_id = (SELECT id FROM ai_plan_drafts WHERE normalized_goal = 'prepare database chapter for graduation thesis' AND user_id = (SELECT id FROM users WHERE username = 'alice_demo'))), 'Run seed SQL in H2 console or MySQL client', 'medium', 'Demo users, tasks, teams, focus logs, and AI plan rows exist.', 1, TIMESTAMP '2026-05-17 18:00:00', 1, TRUE),
    ((SELECT id FROM ai_plan_milestones WHERE title = 'Write thesis explanation' AND draft_id = (SELECT id FROM ai_plan_drafts WHERE normalized_goal = 'prepare database chapter for graduation thesis' AND user_id = (SELECT id FROM users WHERE username = 'alice_demo'))), 'Capture screenshots of seeded data', 'medium', 'Screenshots show table rows and dashboard effect.', 2, TIMESTAMP '2026-05-18 18:00:00', 1, TRUE);

-- Optional quick inspection after seeding.
SELECT 'users' AS table_name, COUNT(*) AS row_count FROM users;
SELECT 'tasks' AS table_name, COUNT(*) AS row_count FROM tasks;
SELECT 'focus_logs' AS table_name, COUNT(*) AS row_count FROM focus_logs;
SELECT 'teams' AS table_name, COUNT(*) AS row_count FROM teams;
SELECT 'team_members' AS table_name, COUNT(*) AS row_count FROM team_members;
SELECT 'ai_plan_drafts' AS table_name, COUNT(*) AS row_count FROM ai_plan_drafts;
SELECT 'ai_plan_milestones' AS table_name, COUNT(*) AS row_count FROM ai_plan_milestones;
SELECT 'ai_plan_tasks' AS table_name, COUNT(*) AS row_count FROM ai_plan_tasks;
SELECT 'push_subscriptions' AS table_name, COUNT(*) AS row_count FROM push_subscriptions;
