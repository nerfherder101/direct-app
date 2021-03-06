/*
 * Copyright (C) 2013 TopCoder Inc., All Rights Reserved.
 */
package com.topcoder.direct.services.project.task.impl;

import javax.persistence.EntityManager;

import com.topcoder.direct.services.project.task.NotificationService;
import com.topcoder.service.user.UserService;
import com.topcoder.util.log.Log;


/**
 * <p>
 * This class is a mock up class for testing the abstract class: BaseJPAService.
 * </p>
 * @author TCSDEVELOPER
 * @version 1.0
 */
public class MockJPATestService extends BaseJPAService implements MockTestService {

    /**
     * <p>
     * Gets the entity manager for unit tests.
     * </p>
     * @return the entity manager.
     */
    public EntityManager getTestEntityManager() {
        return super.getEntityManager();
    }

    /**
     * <p>
     * Gets the notification service for unit tests.
     * </p>
     * @return the notification service for unit tests.
     */
    public NotificationService getTestNotificationService() {
        return super.getNotificationService();
    }

    /**
     * <p>
     * Get the log for unit tests.
     * </p>
     * @return the log for unit tests.
     */
    public Log getTestLog() {
        return super.getLog();
    }

    /**
     * <p>
     * Get the user service for unit tests.
     * </p>
     * @return the user service for unit tests.
     */
    public UserService getTestUserService() {
        return super.getUserService();
    }
}
