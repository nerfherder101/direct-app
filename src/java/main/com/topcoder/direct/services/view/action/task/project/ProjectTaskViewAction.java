/*
 * Copyright (C) 2013 - 2014 TopCoder Inc., All Rights Reserved.
 */
package com.topcoder.direct.services.view.action.task.project;

import com.topcoder.direct.services.project.metadata.entities.dao.DirectProjectMetadata;
import com.topcoder.direct.services.project.milestone.model.Milestone;
import com.topcoder.direct.services.project.milestone.model.MilestoneStatus;
import com.topcoder.direct.services.project.milestone.model.SortOrder;
import com.topcoder.direct.services.project.task.model.ContestDTO;
import com.topcoder.direct.services.project.task.model.MilestoneDTO;
import com.topcoder.direct.services.project.task.model.Task;
import com.topcoder.direct.services.project.task.model.TaskAttachment;
import com.topcoder.direct.services.project.task.model.TaskFilter;
import com.topcoder.direct.services.project.task.model.TaskList;
import com.topcoder.direct.services.project.task.model.TaskPriority;
import com.topcoder.direct.services.project.task.model.TaskStatus;
import com.topcoder.direct.services.project.task.model.UserDTO;
import com.topcoder.direct.services.view.action.FormAction;
import com.topcoder.direct.services.view.action.task.BaseTaskAction;
import com.topcoder.direct.services.view.dto.CommonDTO;
import com.topcoder.direct.services.view.dto.contest.TypedContestBriefDTO;
import com.topcoder.direct.services.view.dto.copilot.CopilotBriefDTO;
import com.topcoder.direct.services.view.dto.project.ProjectBriefDTO;
import com.topcoder.direct.services.view.dto.project.ProjectContestsListDTO;
import com.topcoder.direct.services.view.dto.task.project.TaskDTO;
import com.topcoder.direct.services.view.dto.task.project.TaskFilterDTO;
import com.topcoder.direct.services.view.dto.task.project.TaskGroupDTO;
import com.topcoder.direct.services.view.form.ProjectIdForm;
import com.topcoder.direct.services.view.util.DataProvider;
import com.topcoder.direct.services.view.util.DirectUtils;
import com.topcoder.security.TCSubject;
import com.topcoder.service.permission.Permission;
import org.apache.commons.lang.StringEscapeUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

/**
 * <p>
 * The action for project tasks view.
 * </p>
 *
 * <p>
 * Version 1.1 Release Assembly - TC Cockpit Tasks Management Release 2
 * <ul>
 *     <li>Adds method {@link #addTaskList()}</li>
 *     <li>Adds method {@link #updateTaskList()}</li>
 *     <li>Adds method {@link #deleteTaskList()}</li>
 *     <li>Adds method {@link #resolveTaskList()}</li>
 *     <li>Adds method {@link #getTaskLists()}</li>
 *     <li>Adds method {@link #addNewTask()}</li>
 *     <li>Adds method {@link #updateTask()}</li>
 *     <li>Adds method {@link #getTask()}</li>
 *     <li>Adds method {@link #removeTask()}</li>
 *     <li>Adds method {@link #toggleTask()}</li>
 * </ul>
 * </p>
 *
 * <p>
 * Version 1.2 (TC - Cockpit Tasks Management Assembly 3)
 * <ul>
 *     <li>Adds {@link #filter} and its getter and setter</li>
 *     <li>Adds {@link #group} and its getter and setter</li>
 *     <li>Updates {@link #getTaskLists()} to handle the task list filtering and grouping</li>
 * </ul>
 * </p>
 *
 * <p>
 * Version 1.3 (TopCoder Direct - Change Right Sidebar to pure Ajax)
 * - Removes the statements to populate the right sidebar direct projects and project contests. It's changed to
 * load these data via ajax instead after the page finishes loading.
 * </p>
 *
 * @author GreatKevin, Veve
 * @version 1.3
 */
public class ProjectTaskViewAction extends BaseTaskAction implements FormAction<ProjectIdForm> {

    /**
     *  The MAX DAYS used by the date filtering to calculate a MIN or MAX date from now
     *
     *  @since 1.2
     */
    private static final int MAX_DAYS = 365 * 50;

    /**
     * All the task statuses.
     */
    private static Map<Long, String> taskStatuses = new LinkedHashMap<Long, String>();

    /**
     * All the task priorities.
     */
    private static Map<Long, String> taskPriorities = new LinkedHashMap<Long, String>();

    /**
     * The date format for the task start date and due date.
     */
    private DateFormat taskDateFormat = new SimpleDateFormat("MM/dd/yyyy");

    /**
     * The simple format for the task start date and due date.
     */
    private DateFormat taskDateSimpleFormat = new SimpleDateFormat("dd MMM");

    /**
     * The date format used to display the header of group by due date.
     *
     * @since 1.2
     */
    private DateFormat taskDueDateGroupFormat = new SimpleDateFormat("dd MMM yyyy");

    /**
     * The project ID form.
     */
    private ProjectIdForm formData = new ProjectIdForm();

    /**
     * The task users.
     */
    private Set<Long> taskUserIds = new HashSet<Long>();

    /**
     * The users for the permission chosen.
     *
     * @since 1.1
     */
    private Set<Long> taskPermissionUserIds = new HashSet<Long>();

    /**
     * The contests for the task / task list association.
     *
     * @since 1.1
     */
    private Map<Long, String> projectContests = new LinkedHashMap<Long, String>();

    /**
     * The milestones for the task / task list association.
     *
     * @since 1.1
     */
    private Map<Long, String> projectMilestones = new LinkedHashMap<Long, String>();

    /**
     * The common DTO used for store the right sidebar data.
     *
     * @since 1.1
     */
    private CommonDTO viewData = new CommonDTO();

    /**
     * The task list id.
     *
     * @since 1.1
     */
    private long taskListId;

    /**
     * The new task.
     */
    private TaskDTO task;

    /**
     * The project task lists.
     *
     * @since 1.1
     */
    private List<TaskList> projectTaskLists;

    /**
     * The task list instance.
     *
     * @since 1.1
     */
    private TaskList newTaskList;

    /**
     * The Task Filter DTO used to store the task filters.
     *
     * @since 1.2
     */
    private TaskFilterDTO filter = new TaskFilterDTO();

    /**
     * The Task Group DTO used to store the task grouping type.
     *
     * @since 1.2
     */
    private TaskGroupDTO group = new TaskGroupDTO();

    /**
     * The transaction manager
     */
    PlatformTransactionManager transactionManager;

    /**
     * Static initializer
     */
    static {
        TaskStatus[] statuses = TaskStatus.values();

        for (TaskStatus s : statuses) {
            taskStatuses.put(s.getId(), s.getName());
        }

        TaskPriority[] priorities = TaskPriority.values();

        for (TaskPriority p : priorities) {
            taskPriorities.put(p.getId(), p.getName());
        }
    }

    /**
     * Gets the form data.
     *
     * @return the form data.
     */
    public ProjectIdForm getFormData() {
        return formData;
    }

    /**
     * Sets the form data.
     *
     * @param formData the form data.
     */
    public void setFormData(ProjectIdForm formData) {
        this.formData = formData;
    }


    /**
     * Gets all the task statuses.
     *
     * @return all the task statuses.
     */
    public Map<Long, String> getTaskStatuses() {
        return taskStatuses;
    }


    /**
     * Gets all the task priorities.
     *
     * @return all the task priorities.
     */
    public Map<Long, String> getTaskPriorities() {
        return taskPriorities;
    }

    /**
     * Gets all the task user ids.
     *
     * @return all the task user ids.
     */
    public Set<Long> getTaskUserIds() {
        return taskUserIds;
    }

    /**
     * Gets the new task.
     *
     * @return the new task.
     */
    public TaskDTO getNewTask() {
        return task;
    }

    /**
     * Sets the new task.
     *
     * @param newTask the new task.
     */
    public void setNewTask(TaskDTO newTask) {
        this.task = newTask;
    }


    /**
     * Sets the task.
     *
     * @param task the task to set.
     * @since 1.1
     */
    public void setTask(TaskDTO task) {
        this.task = task;
    }


    /**
     * Gets the task list.
     *
     * @return the task list.
     * @since 1.1
     */
    public TaskList getNewTaskList() {
        return newTaskList;
    }

    /**
     * Sets the task list.
     *
     * @param newTaskList the task list.
     * @since 1.1
     */
    public void setNewTaskList(TaskList newTaskList) {
        this.newTaskList = newTaskList;
    }

    /**
     * Gets the project task lists.
     *
     * @return the project task lists.
     * @since 1.1
     */
    public List<TaskList> getProjectTaskLists() {
        return projectTaskLists;
    }

    /**
     * Gets the users for the task list permission settings.
     *
     * @return the users for the task list permission settings.
     * @since 1.1
     */
    public Set<Long> getTaskPermissionUserIds() {
        return taskPermissionUserIds;
    }

    /**
     * Gets the project contests for setting task/task list association.
     *
     * @return the project contests for setting task/task list association.
     * @since 1.1
     */
    public Map<Long, String> getProjectContests() {
        return projectContests;
    }

    /**
     * Gets the project milestones for setting task/task list association.
     *
     * @return the project milestones for setting task/task list association.
     * @since 1.1
     */
    public Map<Long, String> getProjectMilestones() {
        return projectMilestones;
    }

    /**
     * Sets the task list id.
     *
     * @param taskListId the task list id.
     * @since 1.1
     */
    public void setTaskListId(long taskListId) {
        this.taskListId = taskListId;
    }

    /**
     * Sets the transaction manager.
     *
     * @param transactionManager the transaction manager.
     * @since 1.1
     */
    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
     * Gets the task filter DTO.
     *
     * @return the task filter DTO.
     * @since 1.2
     */
    public TaskFilterDTO getFilter() {
        return filter;
    }

    /**
     * Sets the task filter DTO.
     *
     * @param filter the task filter DTO.
     * @since 1.2
     */
    public void setFilter(TaskFilterDTO filter) {
        this.filter = filter;
    }

    /**
     * Gets the task group DTO.
     *
     * @return the task group DTO.
     * @since 1.2
     */
    public TaskGroupDTO getGroup() {
        return group;
    }

    /**
     * Sets the task group DTO.
     *
     * @param group the task group DTO.
     * @since 1.2
     */
    public void setGroup(TaskGroupDTO group) {
        this.group = group;
    }

    /**
     * Gets the view data for right sidebar.
     *
     * @return the view data for right sidebar.
     * @since 1.1
     */
    public CommonDTO getViewData() {
        return viewData;
    }

    /**
     * Gets the view data for right sidebar.
     *
     * @param viewData the view data for right sidebar.
     * @since 1.1
     */
    public void setViewData(CommonDTO viewData) {
        this.viewData = viewData;
    }

    /**
     * The entry view action for the project tasks page.
     *
     * @throws Exception if there is any error.
     */
    @Override
    protected void executeAction() throws Exception {
        getTaskListService().getDefaultTaskList(DirectUtils.getTCSubjectFromSession().getUserId(),
                                                getFormData().getProjectId());
        setUpTaskAsignees();
        setupTaskPermissionUsers();
        setupProjectMilestones();
        setupProjectContests();

        // right sidebar data
        TCSubject currentUser = DirectUtils.getTCSubjectFromSession();
        final ProjectContestsListDTO projectContests = DataProvider.getProjectContests(currentUser.getUserId(),
                                                                                       getFormData().getProjectId());
        ProjectBriefDTO currentDirectProject;

        if (projectContests.getContests().size() > 0) {
            currentDirectProject = projectContests.getContests().get(0).getContest().getProject();
        } else {
            currentDirectProject = DirectUtils.getCurrentProjectBrief(getProjectServiceFacade(),
                                                                      getFormData().getProjectId());
        }

        getSessionData().setCurrentProjectContext(currentDirectProject);
        getSessionData().setCurrentSelectDirectProjectID(currentDirectProject.getId());
    }

    /**
     * Handles the ajax request to get all the project task lists for the project.
     *
     * @return the result code.
     * @since 1.1
     */
    public String getTaskLists() {
        try {

            TCSubject currentUser = DirectUtils.getTCSubjectFromSession();
            ObjectMapper m = new ObjectMapper();
            m.setDateFormat(taskDateSimpleFormat);

            if(this.taskListId <= 0) {
                // get all the task lists of the project
                TaskFilter taskFilter;
                if(getFilter() != null && getFilter().isApplyFilter()) {
                    taskFilter = getTaskFilterFromFilterDTO(getFilter());
                } else {
                    taskFilter = new TaskFilter();
                }

                taskFilter.setProjectIds(Arrays.asList(new Long[]{getFormData().getProjectId()}));

                // filter the task list out first
                this.projectTaskLists = getTaskListService().getTaskLists(currentUser.getUserId(),
                        taskFilter);

                for(TaskList list : this.projectTaskLists) {
                    // apply filter to task if applyFilter flag is true
                    if(getFilter() != null && getFilter().isApplyFilter()) {
                        filterTasksInTaskList(list, taskFilter);
                    }
                }

                if(getGroup() != null && getGroup().getGroupTypeId() == TaskGroupDTO.GROUP_BY_DUE_DATE) {
                    // group by due date
                    SortedMap<Date, List<Task>> dateListSortedMap = getTaskService().groupTasksByDueDate(this.projectTaskLists);
                    List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();

                    for(Date dueDateKey : dateListSortedMap.keySet()) {
                        Map<String, Object> resultEntry = new HashMap<String, Object>();
                        resultEntry.put("dueDate", taskDueDateGroupFormat.format(dueDateKey));
                        List<Map<String, Object>> tasksGrouped = new ArrayList<Map<String, Object>>();
                        for(Task t : dateListSortedMap.get(dueDateKey)) {

                            if(t.getStatus().equals(TaskStatus.COMPLETED)) {
                                continue;
                            }

                            if(group.getGroupListId() > 0 && group.getGroupListId() != t.getTaskListId()) {
                                continue;
                            }

                            tasksGrouped.add(m.convertValue(t, Map.class));
                        }
                        resultEntry.put("tasks", tasksGrouped);

                        if(tasksGrouped.size() == 0) {
                            // no include empty grouped task
                            continue;
                        }

                        result.add(resultEntry);
                    }
                    setResult(result);

                } else {
                    Map<String, List<Map<String, Object>>> result = new HashMap<String, List<Map<String, Object>>>();
                    List<Map<String, Object>> taskListResult = new ArrayList<Map<String, Object>>();
                    result.put("tasks", taskListResult);
                    for (TaskList tl : projectTaskLists) {
                        taskListResult.add(m.convertValue(tl, Map.class));
                    }
                    setResult(result);
                }

            } else {
                // get a specific task list
                TaskList taskList = getTaskListService().getTaskList(currentUser.getUserId(), this.taskListId);

                if(getFilter().isApplyFilter()) {
                    filterTasksInTaskList(taskList, getTaskFilterFromFilterDTO(getFilter()));
                }

                if(taskList == null) {
                    throw new IllegalArgumentException("The task list to retrieve does not exist");
                }

                setResult(m.convertValue(taskList, Map.class));
            }

        } catch (Throwable e) {
            if (getModel() != null) {
                setResult(e);
            }
        }

        return SUCCESS;
    }


    /**
     * Handles the ajax request to add a new task list.
     *
     * @return the result code.
     * @since 1.1
     */
    public String addTaskList() {
        try {

            long currentUserId = DirectUtils.getTCSubjectFromSession().getUserId();

            // set project id to the projectID in projectID form
            getNewTaskList().setProjectId(getFormData().getProjectId());

            // set task list to be active
            getNewTaskList().setActive(true);

            if(getNewTaskList().getName() != null) {
                // escape the task list name
                getNewTaskList().setName(StringEscapeUtils.escapeHtml(getNewTaskList().getName()));
            }


            if(getNewTaskList().getNotes() != null) {
                // escape the task list notes
                getNewTaskList().setNotes(StringEscapeUtils.escapeHtml(getNewTaskList().getNotes()));
            }

            TaskList taskList = getTaskListService().addTaskList(currentUserId, getNewTaskList());

            taskList = getTaskListService().getTaskList(currentUserId, taskList.getProjectId());

            ObjectMapper m = new ObjectMapper();
            m.setDateFormat(taskDateSimpleFormat);
            Map<String, Object> result = m.convertValue(taskList, Map.class);

            setResult(result);

        } catch (Throwable e) {
            if (getModel() != null) {
                setResult(e);
            }
        }

        return SUCCESS;
    }

    /**
     * Handles the ajax request to update the task list.
     *
     * @return the result code.
     * @since 1.1
     */
    public String updateTaskList() {
        try {

            long currentUserId = DirectUtils.getTCSubjectFromSession().getUserId();


            TaskList taskListToUpdate = getTaskListService().getTaskList(currentUserId, getNewTaskList().getId());

            if(taskListToUpdate == null) {
                throw new IllegalArgumentException("The task list to update does not exist");
            }

            taskListToUpdate.setName(getNewTaskList().getName());
            taskListToUpdate.setNotes(getNewTaskList().getNotes());
            taskListToUpdate.setPermittedUsers(getNewTaskList().getPermittedUsers());

            if (getNewTaskList().getAssociatedToContests() != null && getNewTaskList().getAssociatedToContests().size() > 0) {
                taskListToUpdate.setAssociatedToContests(getNewTaskList().getAssociatedToContests());
                taskListToUpdate.setAssociatedToProjectMilestones(null);
            } else if (getNewTaskList().getAssociatedToProjectMilestones() != null && getNewTaskList().getAssociatedToProjectMilestones().size() > 0) {
                taskListToUpdate.setAssociatedToProjectMilestones(getNewTaskList().getAssociatedToProjectMilestones());
                taskListToUpdate.setAssociatedToContests(null);
            } else {
                taskListToUpdate.setAssociatedToContests(null);
                taskListToUpdate.setAssociatedToProjectMilestones(null);
            }

            getTaskListService().updateTaskList(currentUserId, taskListToUpdate);

            taskListToUpdate = getTaskListService().getTaskList(currentUserId, taskListToUpdate.getId());

            ObjectMapper m = new ObjectMapper();
            m.setDateFormat(taskDateSimpleFormat);
            Map<String, Object> result = m.convertValue(taskListToUpdate, Map.class);

            setResult(result);

        } catch (Throwable e) {
            if (getModel() != null) {
                setResult(e);
            }
        }

        return SUCCESS;
    }

    /**
     * Handles the ajax request to delete the task list.
     *
     * @return the result code.
     * @since 1.1
     */
    public String deleteTaskList() {

        TransactionStatus status = null;

        try {

            long currentUserId = DirectUtils.getTCSubjectFromSession().getUserId();

            DefaultTransactionDefinition def = new DefaultTransactionDefinition();
            def.setName(this.getClass().getName() + ".deleteTaskList");
            def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
            status = transactionManager.getTransaction(def);

            // get the task to delete first
            TaskList taskList = getTaskListService().getTaskList(currentUserId, getNewTaskList().getId());

            if (taskList == null) {
                throw new IllegalArgumentException("The task list to delete does not exist");
            }

            // delete the tasks
            List<Task> tasksToDelete = taskList.getTasks();
            for (Task t : tasksToDelete) {
                for (TaskAttachment ta : t.getAttachments()) {
                    getTaskService().deleteTaskAttachment(currentUserId, ta.getId());
                }
                getTaskService().deleteTask(currentUserId, t.getId());
            }

            // delete the task list finally
            getTaskListService().deleteTaskList(currentUserId, getNewTaskList().getId());

            // commit the transaction
            transactionManager.commit(status);

            Map<String, Long> result = new HashMap<String, Long>();
            result.put("taskListDeletedId", getNewTaskList().getId());

            setResult(result);

        } catch (Throwable e) {
            if (getModel() != null) {
                setResult(e);
            }

            if (status != null && !status.isCompleted()) {
                // roll back if there is exception
                transactionManager.rollback(status);
            }

        }

        return SUCCESS;
    }

    /**
     * Handles the ajax request to resolve the task list.
     *
     * @return the result code.
     * @since 1.1
     */
    public String resolveTaskList() {
        try {

            long currentUserId = DirectUtils.getTCSubjectFromSession().getUserId();

            getTaskListService().resolveTaskList(currentUserId, getNewTaskList().getId());
            Map<String, Long> result = new HashMap<String, Long>();
            result.put("taskListResolvedId", getNewTaskList().getId());

            setResult(result);

        } catch (Throwable e) {
            if (getModel() != null) {
                setResult(e);
            }
        }

        return SUCCESS;
    }

    /**
     * Helper methods to populate all the task users from managers and copilots
     *
     * @throws Exception if any error
     */
    private void setUpTaskAsignees() throws Exception {
        List<DirectProjectMetadata> allMetadata = getMetadataService().getProjectMetadataByProject(formData.getProjectId());

        if (allMetadata != null && allMetadata.size() > 0) {
            for (DirectProjectMetadata data : allMetadata) {
                long keyId = data.getProjectMetadataKey().getId();
                String value = data.getMetadataValue();

                if (value == null || value.trim().length() == 0) {
                    // value does not exist, continue
                    continue;
                }

                if (keyId == 1L || keyId == 2L || keyId == 14L) {
                    // client manager || TopCoder Platform Managers || TopCoder Account Managers
                    taskUserIds.add(Long.parseLong(value));
                }
            }
        }

        List<CopilotBriefDTO> copilots = DataProvider.getCopilotProject(
                DirectUtils.getTCSubjectFromSession().getUserId(), formData.getProjectId()).getCopilots();

        if (copilots != null && copilots.size() > 0) {
            for (CopilotBriefDTO copilot : copilots) {
                taskUserIds.add(copilot.getUserId());
            }
        }
    }

    /**
     * Helper method to setup the permission users.
     *
     * @throws Exception if any error.
     * @since 1.1
     */
    private void setupTaskPermissionUsers() throws Exception {
        final List<Permission> projectPermissions = getPermissionServiceFacade().getPermissionsByProject(
                DirectUtils.getTCSubjectFromSession(), getFormData().getProjectId());
        for(Permission p : projectPermissions) {
            taskPermissionUserIds.add(p.getUserId());
        }
    }

    /**
     * Helper method to setup the project milestones.
     *
     * @throws Exception if any error.
     * @since 1.1
     */
    private void setupProjectMilestones() throws Exception {
        final List<Milestone> allMilestones =
                this.getMilestoneService().getAll(getFormData().getProjectId(), Arrays.asList(MilestoneStatus.values()),
                                                  SortOrder.ASCENDING);
        for(Milestone m : allMilestones) {
            projectMilestones.put(m.getId(), m.getName());
        }
    }

    /**
     * Helper method to setup the project contests.
     *
     * @throws Exception if any error.
     * @since 1.1
     */
    private void setupProjectContests() throws Exception {
        List<TypedContestBriefDTO> contests = DataProvider.getProjectTypedContests(
                DirectUtils.getTCSubjectFromSession().getUserId(),
                getFormData().getProjectId());
        for(TypedContestBriefDTO c : contests) {
            projectContests.put(c.getId(), c.getTitle());
        }
    }


    /**
     * The action operation to handle the ajax request to quickly create a new task.
     *
     * @return the action result code.
     */
    public String addNewTask() {
        try {

            long currentUserId = DirectUtils.getTCSubjectFromSession().getUserId();

            Task task = getTaskService().addTask(currentUserId, createNewTaskFromDTO(getNewTask()));

            task = getTaskService().getTask(currentUserId, task.getId());

            ObjectMapper m = new ObjectMapper();
            m.setDateFormat(taskDateSimpleFormat);
            Map<String, Object> result = m.convertValue(task, Map.class);

            setResult(result);

        } catch (Throwable e) {
            if (getModel() != null) {
                setResult(e);
            }
        }

        return SUCCESS;
    }

    /**
     * Handles the ajax request to update the task.
     *
     * @return the result code.
     * @since 1.1
     */
    public String updateTask() {
        try {

            long currentUserId = DirectUtils.getTCSubjectFromSession().getUserId();

            Task task = getTaskService().getTask(currentUserId, this.task.getId());

            if(task == null) {
                throw new IllegalArgumentException("The task to update does not exist");
            }

            task = updateTaskFromDTO(task, this.task);

            getTaskService().updateTask(currentUserId, task);

            // retrieve again for getting detailed data
            task = getTaskService().getTask(currentUserId, task.getId());

            ObjectMapper m = new ObjectMapper();
            m.setDateFormat(taskDateSimpleFormat);
            Map<String, Object> result = m.convertValue(task, Map.class);

            setResult(result);

        } catch (Throwable e) {
            if (getModel() != null) {
                setResult(e);
            }
        }

        return SUCCESS;
    }

    /**
     * Handles the ajax request to toggle the task.
     *
     * @return the result code.
     * @since 1.1
     */
    public String toggleTask() {
        try {

            long currentUserId = DirectUtils.getTCSubjectFromSession().getUserId();

            Task task = getTaskService().getTask(currentUserId, this.task.getId());

            if(task == null) {
                throw new IllegalArgumentException("The task to resolve does not exist");
            }

            if(task.getStatus() == TaskStatus.COMPLETED) {
                // toggle a completed task, set to Not Start
                task.setStatus(TaskStatus.NOT_STARTED);
            } else {
                task.setStatus(TaskStatus.COMPLETED);
            }

            getTaskService().updateTask(currentUserId, task);

            // retrieve again for getting detailed data
            task = getTaskService().getTask(currentUserId, task.getId());

            ObjectMapper m = new ObjectMapper();
            m.setDateFormat(taskDateSimpleFormat);
            Map<String, Object> result = m.convertValue(task, Map.class);

            setResult(result);

        } catch (Throwable e) {
            if (getModel() != null) {
                setResult(e);
            }
        }

        return SUCCESS;
    }


    /**
     * Handles the ajax request to remove the task.
     *
     * @return the result code.
     * @since 1.1
     */
    public  String removeTask() {
        try {

            long currentUserId = DirectUtils.getTCSubjectFromSession().getUserId();

            getTaskService().deleteTask(currentUserId, this.task.getId());

            Map<String, String> removedTask = new HashMap<String, String>();
            removedTask.put("removedTaskId", String.valueOf(this.task.getId()));

            setResult(removedTask);

        } catch(Throwable e) {
            if(getModel() != null) {
                setResult(e);
            }
        }

        return SUCCESS;
    }

    /**
     * Handles the ajax request to get the task.
     *
     * @return the result code.
     * @since 1.1
     */
    public String getTask() {
        try {

            long currentUserId = DirectUtils.getTCSubjectFromSession().getUserId();

            Task task = getTaskService().getTask(currentUserId, this.task.getId());

            ObjectMapper m = new ObjectMapper();
            m.setDateFormat(taskDateSimpleFormat);
            Map<String, Object> result = m.convertValue(task, Map.class);

            setResult(result);

        } catch (Throwable e) {
            if (getModel() != null) {
                setResult(e);
            }
        }

        return SUCCESS;
    }

    /**
     * Helper method to populate the Task model from TaskDTO.
     *
     * @param task the task model
     * @param taskDTO the <code>TaskDTO</code>
     * @return the populated task model
     * @throws Exception if any error.
     * @since 1.1
     */
    private Task updateTaskFromDTO(Task task, TaskDTO taskDTO) throws Exception {
        // update name
        task.setName(StringEscapeUtils.escapeHtml(taskDTO.getName()));

        // update notes
        if(taskDTO.getNotes() != null && taskDTO.getNotes().trim().length() > 0) {
            // notes set, update notes
            task.setNotes(StringEscapeUtils.escapeHtml(taskDTO.getNotes()));
        } else {
            // update to empty
            task.setNotes(null);
        }

        // update status
        task.setStatus(TaskStatus.forId(taskDTO.getStatusId()));

        // update start date
        if(taskDTO.getStartDate() != null && taskDTO.getStartDate().trim().length() > 0) {
            task.setStartDate(taskDateFormat.parse(taskDTO.getStartDate()));
        } else {
            task.setStartDate(null);
        }

        // update the task due date if exists
        if (taskDTO.getDueDate() != null && taskDTO.getDueDate().trim().length() > 0) {
            task.setDueDate(taskDateFormat.parse(taskDTO.getDueDate()));
        } else {
            task.setDueDate(null);
        }

        // update the task assignee
        if (taskDTO.getAssignUserIds() != null && taskDTO.getAssignUserIds().size() > 0) {
            List<UserDTO> users = new ArrayList<UserDTO>();
            for (Long userId : taskDTO.getAssignUserIds()) {
                UserDTO userDTO = new UserDTO();
                userDTO.setUserId(userId);
                users.add(userDTO);
            }

            task.setAssignees(users);
        }

        // update priority
        if(taskDTO.getPriorityId() >= 0) {
            task.setPriority(TaskPriority.forId(taskDTO.getPriorityId()));
        }

        // update the association
        if (taskDTO.getAssociatedContestId() > 0) {
            // associate task to contest
            ContestDTO contest = new ContestDTO();
            contest.setContestId(taskDTO.getAssociatedContestId());
            task.setAssociatedToContests(Arrays.asList(new ContestDTO[]{contest}));
            task.setAssociatedToProjectMilestones(null);
        } else if (taskDTO.getAssociatedMilestoneId() > 0) {
            // associate task to milestone
            MilestoneDTO milestone = new MilestoneDTO();
            milestone.setMilestoneId(taskDTO.getAssociatedMilestoneId());
            task.setAssociatedToProjectMilestones(Arrays.asList(new MilestoneDTO[]{milestone}));
            task.setAssociatedToContests(null);
        } else {
            task.setAssociatedToProjectMilestones(null);
            task.setAssociatedToContests(null);
        }

        return task;
    }

    /**
     * Helper method to convert a TaskDTO to Task model
     *
     * @param taskDTO the TaskDTO instance
     * @return the converted Task instance
     * @throws Exception if there is any error
     */
    private Task createNewTaskFromDTO(TaskDTO taskDTO) throws Exception {

        Task task = new Task();

        // set the name (escaped)
        task.setName(StringEscapeUtils.escapeHtml(taskDTO.getName()));

        if(taskDTO.getNotes() != null && taskDTO.getNotes().trim().length() > 0) {
            // set the notes (escaped)
            task.setNotes(StringEscapeUtils.escapeHtml(taskDTO.getNotes()));
        } else {
            task.setNotes(null);
        }

        // set the task list id
        task.setTaskListId(taskDTO.getTaskListId());

        // set the TaskStatus enum
        task.setStatus(TaskStatus.forId(taskDTO.getStatusId()));

        // set the TaskPriority enum
        if(taskDTO.getPriorityId() >= 0) {
            task.setPriority(TaskPriority.forId(taskDTO.getPriorityId()));
        } else {
            // set default priority - NORMAL
            task.setPriority(TaskPriority.NORMAL);
        }

        // set the task assignee
        if (taskDTO.getAssignUserIds() != null && taskDTO.getAssignUserIds().size() > 0) {
            List<UserDTO> users = new ArrayList<UserDTO>();
            for (Long userId : taskDTO.getAssignUserIds()) {
                UserDTO userDTO = new UserDTO();
                userDTO.setUserId(userId);
                users.add(userDTO);
            }

            task.setAssignees(users);
        }

        // set the task start date if exists
        if (taskDTO.getStartDate() != null && taskDTO.getStartDate().trim().length() > 0) {
            task.setStartDate(taskDateFormat.parse(taskDTO.getStartDate()));
        } else {
            // not exist
            task.setStartDate(null);
        }

        // set the task due date if exists
        if (taskDTO.getDueDate() != null && taskDTO.getDueDate().trim().length() > 0) {
            task.setDueDate(taskDateFormat.parse(taskDTO.getDueDate()));
        } else {
            // not exist
            task.setDueDate(null);
        }

        // set the creation date
        task.setCreatedDate(new Date());

        if (taskDTO.getAssociatedContestId() > 0) {
            // associate task to contest
            ContestDTO contest = new ContestDTO();
            contest.setContestId(taskDTO.getAssociatedContestId());
            task.setAssociatedToContests(Arrays.asList(new ContestDTO[]{contest}));
        } else if (taskDTO.getAssociatedMilestoneId() > 0) {
            // associate task to milestone
            MilestoneDTO milestone = new MilestoneDTO();
            milestone.setMilestoneId(taskDTO.getAssociatedMilestoneId());
            task.setAssociatedToProjectMilestones(Arrays.asList(new MilestoneDTO[]{milestone}));
        }

        // set the created by
        task.setCreatedBy(String.valueOf(DirectUtils.getTCSubjectFromSession().getUserId()));

        return task;
    }

    /**
     * The comparator to sort the task by creation order.
     */
    private static class TaskComparatorByCreationDate implements Comparator<Task> {
        public int compare(Task t1, Task t2) {
            return t2.getCreatedDate().compareTo(t1.getCreatedDate());
        }
    }

    /**
     * Helper method to transfer a <code>TaskFilterDTO</code> to <code>TaskFilter intance</code>
     *
     * @param filterDTO the task filter DTO
     * @return the TaskFilter instance
     * @throws Exception if there is any error.
     * @since 1.2
     */
    private TaskFilter getTaskFilterFromFilterDTO(TaskFilterDTO filterDTO) throws Exception {
        TaskFilter filter = new TaskFilter();

        // set task name filter
        filter.setName(filterDTO.getName());

        // set assignee id filter
        filter.setAssigneeId(filterDTO.getAssigneeId());

        // set date to filter
        Date dueDateStart = null;
        Date dueDateEnd = null;

        if (filterDTO.getDueType() == TaskFilterDTO.DUE_TODAY) {
            dueDateStart = new Date();
            dueDateEnd = dueDateStart;
        } else if (filterDTO.getDueType() == TaskFilterDTO.DUE_THIS_WEEK) {
            Calendar currentDate = Calendar.getInstance(Locale.US);
            int firstDayOfWeek = currentDate.getFirstDayOfWeek();

            Calendar startDate = Calendar.getInstance(Locale.US);
            startDate.setTime(currentDate.getTime());
            int days = (startDate.get(Calendar.DAY_OF_WEEK) + 7 - firstDayOfWeek) % 7;
            startDate.add(Calendar.DATE, -days);

            Calendar endDate = Calendar.getInstance(Locale.US);
            endDate.setTime(startDate.getTime());
            endDate.add(Calendar.DATE, 6);
            dueDateStart = startDate.getTime();
            dueDateEnd = endDate.getTime();
        } else if (filterDTO.getDueType() == TaskFilterDTO.DUE_NEXT_WEEK || filterDTO.getDueType() == TaskFilterDTO.DUE_LATER) {
            Calendar currentDate = Calendar.getInstance(Locale.US);
            int firstDayOfWeek = currentDate.getFirstDayOfWeek();

            Calendar startDate = Calendar.getInstance(Locale.US);
            startDate.setTime(currentDate.getTime());
            startDate.add(Calendar.DATE, 7);
            startDate.set(Calendar.DAY_OF_WEEK, firstDayOfWeek);

            Calendar endDate = Calendar.getInstance(Locale.US);
            endDate.setTime(currentDate.getTime());
            endDate.add(Calendar.DATE, 7);
            endDate.set(Calendar.DAY_OF_WEEK, (firstDayOfWeek + 6));
            dueDateStart = startDate.getTime();
            dueDateEnd = endDate.getTime();

            if (filterDTO.getDueType() == TaskFilterDTO.DUE_LATER) {
                endDate.add(Calendar.DATE, 1);
                dueDateStart = endDate.getTime();
                // set end to to 50 years from current time
                currentDate.add(Calendar.DATE, MAX_DAYS);
                dueDateEnd = currentDate.getTime();
            }

        } else if (filterDTO.getDueType() == TaskFilterDTO.DUE_PAST) {
            Calendar startDate = Calendar.getInstance(Locale.US);
            Calendar endDate = Calendar.getInstance(Locale.US);
            endDate.setTime(startDate.getTime());
            startDate.add(Calendar.DATE, -MAX_DAYS);
            endDate.add(Calendar.DATE, -1);
            dueDateStart = startDate.getTime();
            dueDateEnd = endDate.getTime();
        }

        if((dueDateStart == null && dueDateEnd == null) &&
                (filterDTO.getDueDateFrom() != null && filterDTO.getDueDateFrom().trim().length() > 0
                        || filterDTO.getDueDateTo() != null && filterDTO.getDueDateTo().trim().length() > 0)) {
            // check date range only if there is no date due type chosen or date due type is ANYTIME
            Calendar startDate = Calendar.getInstance(Locale.US);
            Calendar endDate = Calendar.getInstance(Locale.US);
            startDate.add(Calendar.DATE, -MAX_DAYS);
            endDate.add(Calendar.DATE, MAX_DAYS);
            dueDateStart = startDate.getTime();
            dueDateEnd = endDate.getTime();
            DateFormat dueDateFormat = new SimpleDateFormat("MM/dd/yyyy");
            if(filterDTO.getDueDateFrom() != null && filterDTO.getDueDateFrom().trim().length() > 0) {
                dueDateStart = dueDateFormat.parse(filterDTO.getDueDateFrom());
            }
            if(filterDTO.getDueDateTo() != null && filterDTO.getDueDateTo().trim().length() > 0) {
                dueDateEnd = dueDateFormat.parse(filterDTO.getDueDateTo());
            }

        }
        Calendar cal = Calendar.getInstance(Locale.US);

        // change the time part
        if(dueDateStart != null) {
            cal.setTime(dueDateStart);
            cal.set(Calendar.HOUR_OF_DAY, 0);            // set hour to midnight
            cal.set(Calendar.MINUTE, 0);                 // set minute in hour
            cal.set(Calendar.SECOND, 0);                 // set second in minute
            cal.set(Calendar.MILLISECOND, 0);            // set millis in second
            dueDateStart = cal.getTime();
        }

        if(dueDateEnd != null) {
            cal.setTime(dueDateEnd);
            cal.set(Calendar.HOUR_OF_DAY, 23);            // set hour to max
            cal.set(Calendar.MINUTE, 59);                 // set minute to max
            cal.set(Calendar.SECOND, 59);                 // set second to max
            dueDateEnd = cal.getTime();
        }

        filter.setDueDateFrom(dueDateStart);
        filter.setDueDateTo(dueDateEnd);

        // set priority
        if(filterDTO.getPriorityIds() != null) {
            List<TaskPriority> priorities = new ArrayList<TaskPriority>();
            for(long pid : filterDTO.getPriorityIds()) {
                priorities.add(TaskPriority.forId(pid));
            }
            filter.setPriorities(priorities);
        }

        // set status
        if(filterDTO.getStatusIds() != null) {
            List<TaskStatus> statuses = new ArrayList<TaskStatus>();
            for(long sid: filterDTO.getStatusIds()) {
                statuses.add(TaskStatus.forId(sid));
            }
            filter.setStatuses(statuses);
        }

        // set contest ids
        filter.setAssociatedToContestIds(filterDTO.getContestIds());

        // set milestone ids
        filter.setAssociatedToProjectMilestoneIds(filterDTO.getMilestoneIds());

        return filter;
    }


    /**
     * Helper method to filter the tasks in the task list with the specified TaskFilter.
     *
     * @param list the task list
     * @param filter the task filter
     * @since 1.2
     */
    private void filterTasksInTaskList(TaskList list, TaskFilter filter) {
        List<Task> filteredTasks = new ArrayList<Task>();
        List<Task> tasks = list.getTasks();

        for(Task t : tasks) {

            // 1) NAME
            if (filter.getName() != null && filter.getName().trim().length() > 0) {
                if (t.getName().toLowerCase().indexOf(filter.getName().toLowerCase()) == -1) {
                    // do not contain the name, bypass
                    continue;
                }
            }


            // 2) ASSIGNEE
            if (filter.getAssigneeId() != null && filter.getAssigneeId() > 0) {
                List<UserDTO> assignees = t.getAssignees();
                boolean matched = false;
                for (UserDTO ud : assignees) {
                    if (ud.getUserId() == filter.getAssigneeId()) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    // no match, bypass
                    continue;
                }
            }


            // 3) DUE
            if (filter.getDueDateFrom() != null && filter.getDueDateTo() != null) {
                if (t.getDueDate() == null) {
                    continue;
                }

                if (t.getDueDate().before(filter.getDueDateFrom()) || t.getDueDate().after(filter.getDueDateTo())) {
                    continue;
                }
            }


            // 4) PRIORITIES
            if (filter.getPriorities() == null || filter.getPriorities().size() == 0) {
                continue;
            } else if (!filter.getPriorities().contains(t.getPriority())) {
                continue;
            }

            // 5) STATUS
            if (filter.getStatuses() == null || filter.getStatuses().size() == 0) {
                continue;
            } else if (!filter.getStatuses().contains(t.getStatus())) {
                continue;
            }

            // 6) MILESTONE ASSOCIATION
            if (filter.getAssociatedToProjectMilestoneIds() != null && filter.getAssociatedToProjectMilestoneIds().size() > 0) {
                // do the filters
                boolean matched = false;
                for (MilestoneDTO md : t.getAssociatedToProjectMilestones()) {
                    if (filter.getAssociatedToProjectMilestoneIds().contains(md.getMilestoneId())) {
                        matched = true;
                        break;
                    }
                }

                if (!matched) {
                    continue;
                }
            }

            // 7) CONTEST ASSOCIATION
            if (filter.getAssociatedToContestIds() != null && filter.getAssociatedToContestIds().size() > 0) {
                // do the filters
                boolean matched = false;
                for (ContestDTO cd : t.getAssociatedToContests()) {
                    if (filter.getAssociatedToContestIds().contains(cd.getContestId())) {
                        matched = true;
                        break;
                    }
                }

                if (!matched) {
                    continue;
                }
            }

            // all matched
            filteredTasks.add(t);
        }

        list.setTasks(filteredTasks);
    }
}
