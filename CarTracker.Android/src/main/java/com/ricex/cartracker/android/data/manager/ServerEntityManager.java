package com.ricex.cartracker.android.data.manager;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.ricex.cartracker.android.data.entity.ReaderLog;
import com.ricex.cartracker.android.data.entity.ServerEntity;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by Mitchell on 2016-11-02.
 */
public abstract class ServerEntityManager<T extends ServerEntity> extends EntityManager<T> {

    private Dao<T, Long> entityDao;

    protected ServerEntityManager(Dao<T, Long> entityDao) {
        super(entityDao);
        this.entityDao = entityDao;
    }

    /** Returns a list of the entities that haven't been synced with the server yet
     *
     * @return The entities that haven't been synced with the server yet, or null if
     *      an error occured
     */
    public List<T> getUnsynced(long maxResults) {
        try {
            QueryBuilder<T, Long> queryBuilder = getQueryBuilder();
            Where<T, Long> where = queryBuilder.where();
            where.and(
                    where.eq(ServerEntity.PROPERTY_SYNCED_WITH_SERVER, false),
                    where.or(
                            where.isNull(ServerEntity.PROPERTY_LAST_ATTEMPTED_SYNC),
                            where.lt(ServerEntity.PROPERTY_LAST_ATTEMPTED_SYNC, getLastSyncedCutoff()
                            )
                    )
            );
            queryBuilder.limit(maxResults);
            return executeQuery(queryBuilder);
        }
        catch (SQLException e) {
            logException("Failed to fetch unsynced entities.", e);
            return null;
        }
    }

    protected Date getLastSyncedCutoff() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, -2);
        return cal.getTime();
    }


}
