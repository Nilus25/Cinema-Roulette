ALTER TABLE media_items ADD COLUMN original_title TEXT;
ALTER TABLE media_items ADD COLUMN runtime_mins INT CHECK (runtime_mins IS NULL OR runtime_mins >= 0);
ALTER TABLE media_items ADD COLUMN num_votes INT CHECK (num_votes IS NULL OR num_votes >= 0);
