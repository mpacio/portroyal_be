package com.matteopaciolla.portroyal.util.io;

import com.googlecode.jcsv.writer.CSVEntryConverter;
import com.matteopaciolla.portroyal.core.MoveRecord;

public class MoveRecordConverter implements CSVEntryConverter<MoveRecord> {

    @Override
    public String[] convertEntry(MoveRecord moveRecord) {
        return new String[]{
                String.valueOf(moveRecord.getTimeIndex()),
                String.valueOf(moveRecord.getRunningPlayerIndex()),
                moveRecord.getMove().name(),
                String.valueOf(moveRecord.getChoiceIndex()),
                String.valueOf(moveRecord.getPickPlayerIndex()),
                moveRecord.getExpeditionEmployees() != null ? moveRecord.getExpeditionEmployees().toString() : "",
                moveRecord.getNotes() != null ? moveRecord.getNotes() : ""
        };
    }
}
