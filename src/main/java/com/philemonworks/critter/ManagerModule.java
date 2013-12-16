package com.philemonworks.critter;

import com.google.inject.AbstractModule;
import com.philemonworks.critter.dao.RecordingDao;
import com.philemonworks.critter.dao.RecordingDaoMemoryImpl;
import com.philemonworks.critter.dao.RuleDao;
import com.philemonworks.critter.dao.RuleDaoMemoryImpl;
import com.philemonworks.critter.dao.mongo.MongoModule;
import com.philemonworks.critter.dao.mongo.RecordingDaoMongoImpl;
import com.philemonworks.critter.dao.mongo.RuleDaoMongoImpl;
import com.philemonworks.critter.dao.sql.RecordingDaoSqlImpl;
import com.philemonworks.critter.dao.sql.RuleDaoSqlImpl;
import com.philemonworks.critter.db.DbCreator;
import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Properties;

/**
* Represents the Critter manager module.
*
* @author jcraane
*/
final class ManagerModule extends AbstractModule {
    private static final Logger LOG = LoggerFactory.getLogger(ManagerModule.class);
    private final Properties properties;
    private final TrafficManager trafficManager;

    ManagerModule(final Properties properties, final TrafficManager trafficManager) {
        this.properties = properties;
        this.trafficManager = trafficManager;
    }

    @Override
    protected void configure() {
        RuleDao ruleDao;
        RecordingDao recordingDao;
        if (Boolean.parseBoolean((String) properties.get("rule.database.h2.enabled"))) {
            ruleDao = new RuleDaoSqlImpl();
            recordingDao = new RecordingDaoSqlImpl();
            DbCreator.create(createAndBindDataSource());
        } else if (properties.containsKey(MongoModule.HOST)) {
            LOG.info("Using MongoDB rules database");
            this.install(new MongoModule(properties));
            ruleDao = new RuleDaoMongoImpl();
            recordingDao = new RecordingDaoMongoImpl();
        } else {
            LOG.info("Using in memory rules database");
            ruleDao = new RuleDaoMemoryImpl();
            recordingDao = new RecordingDaoMemoryImpl();
        }

        this.bind(TrafficManager.class).toInstance(trafficManager);
        this.bind(RuleDao.class).toInstance(ruleDao);
        this.bind(RecordingDao.class).toInstance(recordingDao);
    }

    private DataSource createAndBindDataSource() {
        final BasicDataSource dataSource = new BasicDataSource();
        dataSource.setUrl("jdbc:h2:critter");
        this.bind(DataSource.class).toInstance(dataSource);
        return dataSource;
    }
}
