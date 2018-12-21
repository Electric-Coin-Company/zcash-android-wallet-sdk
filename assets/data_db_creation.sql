CREATE TABLE IF NOT EXISTS blocks (
            height INTEGER PRIMARY KEY,
            time INTEGER,
            sapling_tree BLOB
        );

CREATE TABLE IF NOT EXISTS transactions (
            id_tx INTEGER PRIMARY KEY,
            txid BLOB NOT NULL UNIQUE,
            block INTEGER,
            raw BLOB,
            FOREIGN KEY (block) REFERENCES blocks(height)
        );

CREATE TABLE IF NOT EXISTS received_notes (
            id_note INTEGER PRIMARY KEY,
            tx INTEGER NOT NULL,
            output_index INTEGER NOT NULL,
            account INTEGER NOT NULL,
            diversifier BLOB NOT NULL,
            value INTEGER NOT NULL,
            rcm BLOB NOT NULL,
            nf BLOB NOT NULL UNIQUE,
            memo BLOB,
            spent INTEGER,
            FOREIGN KEY (tx) REFERENCES transactions(id_tx),
            FOREIGN KEY (spent) REFERENCES transactions(id_tx),
            CONSTRAINT tx_output UNIQUE (tx, output_index)
        );

CREATE TABLE IF NOT EXISTS sapling_witnesses (
            id_witness INTEGER PRIMARY KEY,
            note INTEGER NOT NULL,
            block INTEGER NOT NULL,
            witness BLOB NOT NULL,
            FOREIGN KEY (note) REFERENCES received_notes(id_note),
            FOREIGN KEY (block) REFERENCES blocks(height),
            CONSTRAINT witness_height UNIQUE (note, block)
        );
