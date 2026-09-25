package com.matteopaciolla.portroyal.util.io;

import com.googlecode.jcsv.reader.CSVEntryParser;
import com.matteopaciolla.portroyal.core.enums.MoveAction;
import com.matteopaciolla.portroyal.core.MoveRecord;
import com.matteopaciolla.portroyal.core.cards.enums.ExpeditionEmployee;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MoveRecordParser implements CSVEntryParser<MoveRecord> {

    @Override
    public MoveRecord parseEntry(String... data) {
        if (data.length == 7) {
            MoveRecord moveRecord = new MoveRecord();
            moveRecord.setTimeIndex(Integer.parseInt(data[0]));
            moveRecord.setRunningPlayerIndex(Integer.parseInt(data[1]));
            moveRecord.setMove(MoveAction.valueOf(data[2]));
            moveRecord.setChoiceIndex(Integer.parseInt(data[3]));
            moveRecord.setPickPlayerIndex(Integer.parseInt(data[4]));
            moveRecord.setExpeditionEmployees(parseExpeditionEmployees(data[5]));
            moveRecord.setNotes(data[6]);
            return moveRecord;
        } else {
            throw new IllegalArgumentException("Invalid data: " + Arrays.toString(data));
        }
    }

    private List<ExpeditionEmployee> parseExpeditionEmployees(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        List<ExpeditionEmployee> expeditionEmployees = new ArrayList<>();
        // remove the brackets
        data = data.substring(1, data.length() - 1);
        String[] split = data.split(",");
        for (String s : split) {
            expeditionEmployees.add(ExpeditionEmployee.valueOf(s.trim()));
        }
        return expeditionEmployees;
    }
}
