package org.smartregister.chw.dao;

import android.util.Pair;

import org.smartregister.dao.AbstractDao;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FamilyDao extends AbstractDao {

    public static Map<String, Integer> getFamilyServiceSchedule(String familyBaseEntityID) {
        String sql = "select visit_state , count(*) totals  from (  " +
                "SELECT s.base_entity_id , m.relational_id , CASE  " +
                "WHEN completion_date  is NOT NULL  THEN  'DONE'  " +
                "WHEN not_done_date is NOT NULL THEN  'NOT_VISIT_THIS_MONTH' " +
                "WHEN strftime('%Y-%m-%d') BETWEEN due_date AND over_due_date THEN  'DUE' " +
                "WHEN strftime('%Y-%m-%d') BETWEEN over_due_date AND expiry_date THEN  'OVERDUE' " +
                "WHEN strftime('%Y-%m-%d')  >= expiry_date  THEN  'EXPIRY'  end  visit_state " +
                "FROM schedule_service s " +
                "LEFT join ec_family_member m on s.base_entity_id  = m.base_entity_id COLLATE NOCASE " +
                "WHERE visit_state is NOT NULL " +
                ") counters   where " +
                " ((counters.relational_id = '" + familyBaseEntityID + "' COLLATE NOCASE) or " +
                " (counters.base_entity_id = '" + familyBaseEntityID + "' COLLATE NOCASE)) " +
                "group by visit_state";


        DataMap<Pair<String, Integer>> dataMap = c -> Pair.create(getCursorValue(c, "visit_state"), getCursorIntValue(c, "totals"));

        Map<String, Integer> visits = new HashMap<>();
        List<Pair<String, Integer>> pairs = AbstractDao.readData(sql, dataMap);
        if (pairs == null || pairs.size() == 0)
            return visits;

        for (Pair<String, Integer> pair : pairs) {
            visits.put(pair.first, pair.second);
        }

        return visits;
    }

    public static String getMemberDueStatus(String memberBaseEntityId) {
        String sql = "select visit_state from (  " +
                "SELECT m.relational_id , CASE  " +
                "WHEN completion_date  is NOT NULL  THEN  'DONE'  " +
                "WHEN not_done_date is NOT NULL THEN  'NOT_VISIT_THIS_MONTH' " +
                "WHEN strftime('%Y-%m-%d') BETWEEN due_date AND over_due_date THEN  'DUE' " +
                "WHEN strftime('%Y-%m-%d') BETWEEN over_due_date AND expiry_date THEN  'OVERDUE' " +
                "WHEN strftime('%Y-%m-%d')  >= expiry_date  THEN  'EXPIRY'  end  visit_state " +
                "FROM schedule_service s " +
                "inner join ec_family_member m on s.base_entity_id  ='" + memberBaseEntityId + "'" + " COLLATE NOCASE " +
                "WHERE visit_state is NOT NULL " +
                ") counters " +
                "group by visit_state";

        DataMap<String> dataMap = c -> getCursorValue(c, "visit_state");
        String dueStatus;
        List<String> dueStatusListString = AbstractDao.readData(sql, dataMap);
        if (dueStatusListString.size() > 0) {
            dueStatus = dueStatusListString.get(0);
        } else {
            dueStatus = "";
        }

        return dueStatus;

    }

    public static long getFamilyCreateDate(String familyBaseEntityID) {
        String sql = "select eventDate from event where eventType = 'Family Registration' and " +
                "baseEntityId = '" + familyBaseEntityID + "' order by eventDate desc limit 1";

        DataMap<Date> dataMap = c -> getCursorValueAsDate(c, "eventDate", getDobDateFormat());
        List<Date> res = AbstractDao.readData(sql, dataMap);
        if (res == null || res.size() == 0)
            return 0;

        return res.get(0).getTime();
    }

    public static boolean isFamily(String baseEntityID) {
        String sql = "select count(*) count from ec_family where base_entity_id = '" + baseEntityID + "'";

        DataMap<Integer> dataMap = cursor -> getCursorIntValue(cursor, "count");

        List<Integer> res = readData(sql, dataMap);
        if (res == null || res.size() != 1)
            return false;

        return res.get(0) > 0;
    }
}