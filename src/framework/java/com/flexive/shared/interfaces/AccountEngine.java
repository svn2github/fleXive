/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License version 2.1 or higher as published by the Free Software Foundation.
 *
 *  The GNU Lesser General Public License can be found at
 *  http://www.gnu.org/licenses/lgpl.html.
 *  A copy is found in the textfile LGPL.txt and important notices to the
 *  license from the author are found in LICENSE.txt distributed with
 *  these libraries.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  For further information about UCS - unique computing solutions gmbh,
 *  please see the company website: http://www.ucs.at
 *
 *  For further information about [fleXive](R), please see the
 *  project website: http://www.flexive.org
 *
 *
 *  This copyright notice MUST APPEAR in all copies of the file!
 ***************************************************************/
package com.flexive.shared.interfaces;

import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.security.*;

import javax.ejb.Remote;
import java.util.Date;
import java.util.List;

/**
 * Account management
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Remote
public interface AccountEngine {

    /**
     * Perform a login
     *
     * @param username the username
     * @param password the password
     * @param takeOver the take over flag
     * @throws FxLoginFailedException  on errors
     * @throws FxAccountInUseException on errors
     */
    void login(String username, String password, boolean takeOver)
            throws FxLoginFailedException, FxAccountInUseException;

    /**
     * Check the password of the currently logged-in user. The match can only be performed if the current UserTicket
     * matches with the ID of the login account's name
     *
     * @param username the user name
     * @param password the password
     * @param currentTicket the current UserTicket to check
     * @return returns true if the password matches
     * @throws FxLoginFailedException on errors
     * @throws FxDbException on db errors
     */
    boolean loginCheck(String username, String password, UserTicket currentTicket) throws FxLoginFailedException, FxDbException;

    /**
     * Returns all currently active UserTickets.
     *
     * @return all active UserTickets
     */
    List<UserTicket> getActiveUserTickets();

    /**
     * Logout function.
     *
     * @throws FxLogoutFailedException on errors
     */
    void logout() throws FxLogoutFailedException;

    /**
     * Loads a user specified by its unique id.
     * <p/>
     * This function may be called by anyone and performs no security checks.
     *
     * @param id the unique id of the user to load
     *           be retrieved.
     * @return the account
     * @throws com.flexive.shared.exceptions.FxNotFoundException
     *                                if the user does not exist
     * @throws com.flexive.shared.exceptions.FxLoadException
     *                                if the load failed
     * @throws FxApplicationException on errors
     */
    Account load(long id) throws FxApplicationException;

    /**
     * Loads a user.
     * <p/>
     * This function may be called by anyone and performs no security checks.
     *
     * @param loginName the login name of the user to load
     * @return the account
     * @throws FxNotFoundException    if the user does not exist
     * @throws FxLoadException        if the load failed
     * @throws FxApplicationException on errors
     */
    Account load(String loginName) throws FxApplicationException;

    /**
     * Load the account that belongs to the given contact data
     *
     * @param contactDataPK contact data
     * @return the account that belongs to the given contact data
     * @throws FxApplicationException on errors loading the account or if no account exists for the contact data
     */
    Account loadForContactData(FxPK contactDataPK) throws FxApplicationException;

    /**
     * Load the password hash for the given account ID. May only be called by global supervisors.
     *
     * @param accountId the account ID
     * @return          the hashed password
     * @throws FxApplicationException   on errors
     * @since 3.1.6
     */
    String getPasswordHash(long accountId) throws FxApplicationException;

    /**
     * Gets the user ticket for the current request.
     *
     * @return the user ticket for the current request.
     */
    UserTicket getUserTicket();

    /**
     * Returns a guest user ticket.
     *
     * @return  a guest user ticket.
     * @since 3.1
     */
    UserTicket getGuestTicket();

    /**
     * Gets the groups a user is assigned to.
     * <p/>
     * A user may only see the groups assigned to other users within his mandator.
     * GLOBAL_SUPERVISOR may get the groups for all users.
     *
     * @param accountId the user to get the groupd for
     * @return the groups a user is assigned to
     * @throws FxLoadException        if the load failed
     * @throws FxNotFoundException    if the user does not exist
     * @throws FxNoAccessException    if the caller lacks the permissions to load the groups
     * @throws FxApplicationException on errors
     */
    List<UserGroup> getGroups(long accountId) throws FxApplicationException;


    /**
     * Loads all roles that a user is assigned to.
     * <p/>
     * Users may only query roles of users within the same mandator domain.<br>
     * GLOBAL_SUPERVISOR may get the roles of all users.
     *
     * @param accountId the unique user id to get the roles for
     * @param mode      MODE_USER:   get all roles the USER himself is assigned to<br>
     *                  MODE_GROUPS: get all roles from the groups that the user belongs to<br>
     *                  MODE_ALL:    get all roles the user belongs to from his groups, or direct assignment
     * @return the roles assigned to the given user
     * @throws FxLoadException        if the load failed
     * @throws FxNotFoundException    if the user does not exist
     * @throws FxNoAccessException    if the caller lacks the permissions to load the roles
     * @throws FxApplicationException on errors
     */
    List<Role> getRoles(long accountId, RoleLoadMode mode) throws FxApplicationException;

    /**
     * Creates a new user.
     * <p/>
     * Role and Groups can be added after creation.<br>
     * Only callers in ROLE_USER_MANAGEMENTS may create users, and only for their mandatorId.<br>
     * GLOBAL_SUPERVISOR may create users for all mandators.
     *
     * @param account  the account to be created. Use an {@link AccountEdit} object for easier account creation.
     * @param password the user's password
     * @return the ID of the created user
     * @throws FxCreateException           if the create failed
     * @throws FxInvalidParameterException if a parameter is invalid (mandatorId, guiLanguage, contentLanguage)
     * @throws FxNoAccessException         if the caller lacks the permissions to create the user
     * @throws FxEntryExistsException      if a user with the given login name already exists
     * @throws FxApplicationException      on errors
     */
    long create(Account account, String password) throws FxApplicationException;

    /**
     * Creates a new user.
     * <p/>
     * Role and Groups can be added after creation.<br>
     * Only callers in ROLE_USER_MANAGEMENTS may create users, and only for their mandatorId.<br>
     * GLOBAL_SUPERVISOR may create users for all mandators.
     *
     * @param account  the account to be created. Use an {@link AccountEdit} object for easier account creation.
     * @param password the user's password
     * @param hashPassword whether the password should be hashed (set to false for importing users, otherwise
     *                     this should always be true)
     * @return the ID of the created user
     * @throws FxCreateException           if the create failed
     * @throws FxInvalidParameterException if a parameter is invalid (mandatorId, guiLanguage, contentLanguage)
     * @throws FxNoAccessException         if the caller lacks the permissions to create the user
     * @throws FxEntryExistsException      if a user with the given login name already exists
     * @throws FxApplicationException      on errors
     * @since 3.1.6
     */
    long create(Account account, String password, boolean hashPassword) throws FxApplicationException;

    /**
     * Removes a user.
     * <p/>
     * The caller must be in role AccountManagement to remove a user belonging to his mandator.<br>
     * GlobalSupervisor may remove users belonging to any mandator.<br>
     * USER_GUEST and USER_GLOBAL_SUPERVISOR may not be removed in any case.
     *
     * @param accountId the id of the user to remove
     * @throws FxNotFoundException    if the given user does not exist
     * @throws FxNoAccessException    if the caller lacks the permissions to remove the user
     * @throws FxRemoveException      if the remove failed
     * @throws FxApplicationException on errors
     */
    void remove(long accountId) throws FxApplicationException;

    /**
     * Sets the roles a user is in.
     * To set roles the caller must be in role AccountManagement, and may only update users belonging
     * to his mandator. GlobalSupervisor may set the roles for all users in the system.
     *
     * @param accountId the user to set the roles for
     * @param roles     the roles to set, the array may contain undefined roles (=0) values (which are skipped) to make it
     *                  easier to build the list. <br>
     *                  Duplicated roles are discarded.
     * @throws FxNoAccessException    if the calling user lacks the permissions to set the roles for the given group
     * @throws FxNotFoundException    if the group does not exist
     * @throws FxUpdateException      if setting the roles failed
     * @throws FxApplicationException on errors
     */
    void setRoles(long accountId, long... roles) throws FxApplicationException;

    /**
     * Adds a role for a user.
     * To set roles the caller must be in role AccountManagement, and may only update users belonging
     * to his mandator. GlobalSupervisor may set the roles for all users in the system.
     *
     * @param accountId the user ID
     * @param roleId    the role ID to be added
     * @throws FxNoAccessException    if the calling user lacks the permissions to set the roles for the given group
     * @throws FxNotFoundException    if the group does not exist
     * @throws FxUpdateException      if setting the roles failed
     * @throws FxApplicationException on errors
     */
    void addRole(long accountId, long roleId) throws FxApplicationException;

    /**
     * Sets the roles a user is in.
     * <p/>
     * To set roles the caller must be in role ROLE_ROLE_MANAGEMENT, and may only update users belonging
     * to his mandator. GROUP_GLOBAL_SUPERVISOR may set the roles for all users in the system.
     *
     * @param accountId the user to set the roles for
     * @param roles     the roles to set, the array may contain ROLE_UNDEFINED (=0) values (which are skipped) to make it
     *                  easier to build the list. <br>
     *                  Duplicated roles are discarded.
     * @throws FxNoAccessException    if the calling user lacks the permissions to set the roles for the given group
     * @throws FxNotFoundException    if the group does not exist
     * @throws FxUpdateException      if setting the roles failed
     * @throws FxApplicationException on errors
     */
    void setRoles(long accountId, List<Role> roles) throws FxApplicationException;

    /**
     * Sets the groups a user defined by its unique id belongs to.
     * <p/>
     * The caller must be in role ROLE_GROUP_MANAGEMENT or AccountManagement, and may
     * only update users belonging to his mandator. He may only assign groups that also belong to
     * his mandator, plus GROUP_EVERYONE and GROUP_OWNER.<br>
     * GROUP_GLOBAL_SUPERVISOR may set all groups for all users.
     *
     * @param accountId the accountId
     * @param groups    the groups the user should belong to
     * @throws FxNoAccessException    if the calling user lacks the permissions to set the groups
     * @throws FxNotFoundException    if the user does not exist
     * @throws FxUpdateException      if setting the groups failed
     * @throws FxApplicationException on errors
     */
    void setGroups(long accountId, long... groups) throws FxApplicationException;

    /**
     * Adds a usergroup to a user.
     * <p/>
     * The caller must be in role ROLE_GROUP_MANAGEMENT or AccountManagement, and may
     * only update users belonging to his mandator. He may only assign groups that also belong to
     * his mandator, plus GROUP_EVERYONE and GROUP_OWNER.<br>
     * GROUP_GLOBAL_SUPERVISOR may set all groups for all users.
     *
     * @param accountId the accountId
     * @param groupId   the groupID to be added
     * @throws FxNoAccessException    if the calling user lacks the permissions to set the groups
     * @throws FxNotFoundException    if the user does not exist
     * @throws FxUpdateException      if setting the groups failed
     * @throws FxApplicationException on errors
     */
    void addGroup(long accountId, long groupId) throws FxApplicationException;

    /**
     * Sets the groups a user defined by its unique id belongs to.
     * <p/>
     * The caller must be in role ROLE_GROUP_MANAGEMENT or AccountManagement, and may
     * only update users belonging to his mandator. He may only assign groups that also belong to
     * his mandator, plus GROUP_EVERYONE and GROUP_OWNER.<br>
     * GROUP_GLOBAL_SUPERVISOR may set all groups for all users.
     *
     * @param accountId the accountId
     * @param groups    the groups the user should belong to
     * @throws FxNoAccessException    if the calling user lacks the permissions to set the groups
     * @throws FxNotFoundException    if the user does not exist
     * @throws FxUpdateException      if setting the groups failed
     * @throws FxApplicationException on errors
     */
    void setGroups(long accountId, List<UserGroup> groups) throws FxApplicationException;


    /**
     * Loads all users matching the parameters.
     * <p/>
     * The Name, LoginName and Email are compared case insensitive.<br>
     * The caller may only search users within its own mandator<br>.
     * Users in the group GROUP_GLOBAL_SUPERVISOR may load users within all mandators.
     *
     * @param name        (a substring of) the name of the users to load, or null if the name should not filter the result
     * @param loginName   (a substring of) the login name of the users to load, or null if the login name should not filter
     *                    the result
     * @param email       (a substring of) the email of the users to load, or null if the email should not filter the result
     * @param isActive    true|false to restrict by the active flag, or null if the active flag should not filter the result
     * @param isConfirmed true | false to restrict by the confirmed flag, or null if the confirmed flag should not filter
     *                    the result
     * @param mandatorId  the function returns only users belonging to this mandator. If set to null the mandator of the
     *                    calling user is used. GROUP_GLOBAL_SUPERVISOR may use -1 to load users within all mandators, all other callers may
     *                    only load users within the mandator they belong to, or a FxNoAccessException is thrown.
     * @param isInRole    if set the function only loads users which are in all specified roles. The result will be empty
     *                    if a invalid role id is used.
     * @param isInGroup   if set the function only loads users which belong to all specified groups. The result will be empty
     *                    if a invalid group id is used.
     * @param startIdx    the start index in the result, 0 based
     * @param maxEntries  the maximum amount of users returned by the funktion (-1 for all), starting at startIdx
     * @return all matching accounts
     * @throws FxNoAccessException    if the caller may not load users of the specified mandator
     * @throws FxLoadException        if the load failed
     * @throws FxApplicationException on errors
     */
    List<Account> loadAll(String name, String loginName, String email, Boolean isActive,
                      Boolean isConfirmed, Long mandatorId, int[] isInRole, long[] isInGroup, int startIdx, int maxEntries)
            throws FxApplicationException;

    /**
     * Loads all accounts of a mandator.
     *
     * @param mandatorId the mandator ID
     * @return all accounts of the mandator.
     * @throws FxApplicationException on errors
     */
    List<Account> loadAll(long mandatorId) throws FxApplicationException;

    /**
     * Loads all accounts. Unless the calling user is a global supervisor, this call returns the same result
     * as calling {@link #loadAll(long)} with the user mandator ID.
     *
     * @return all accounts.
     * @throws FxApplicationException on errors
     */
    List<Account> loadAll() throws FxApplicationException;

    /**
     * Returns number of users matching the parameters.
     * <p/>
     * The caller may only search users within its own mandator<br>.
     * Users in the group GROUP_GLOBAL_SUPERVISOR may load users within all mandators.
     *
     * @param name        (a substring of) the name of the users to load,
     *                    or null if the name should not filter the result
     * @param loginName   (a substring of) the login name of the users to load,
     *                    or null if the login name should not filter the result
     * @param email       (a substring of) the email of the users to load,
     *                    or null if the email should not filter the result
     * @param isActive    true|false to restrict by the active flag,
     *                    or null if the active flag should not filter the result
     * @param isConfirmed true | false to restrict by the confirmed flag,
     *                    or null if the confirmed flag should not filter the result
     * @param mandatorId  the function returns only users belonging to this mandator. If set to null the mandator of the
     *                    calling user is used. GROUP_GLOBAL_SUPERVISOR may use -1 to load users within all mandators,
     *                    all other callers may only load users within the mandator they belong to,
     *                    or a FxNoAccessException is thrown.
     * @param isInRole    if set the function only loads users which are in all specified roles.
     *                    The result will be empty if an invalid role id is used.
     * @param isInGroup   if set the function only loads users which belong to all specified groups.
     *                    The result will be empty if a invalid group id is used.
     * @return The number of users matching the given parameters
     * @throws FxNoAccessException    if the caller may not load users of the specified mandator
     * @throws FxLoadException        if the load failed
     * @throws FxApplicationException on errors
     */
    int getAccountMatches(String name, String loginName, String email, Boolean isActive,
                          Boolean isConfirmed, Long mandatorId, int[] isInRole, long[] isInGroup)
            throws FxApplicationException;

    /**
     * Updates the data of a user specified by its unique id.
     * <p/>
     * Only callers in ROLE_USER_MANAGEMENTS may create users, and only for their mandator.<br>
     * GLOBAL_SUPERVISOR may create users for all mandators.<br>
     * Any user can change HIS OWN password, email, contentLanguage and guiLanguage using this function,
     * but setting any other parameters will cause a FxNoAccessException.
     *
     * @param accountId       the unique id of the user to update
     * @param password        the new password, or null if the old value should be kept
     * @param defaultNode     the new defaultNode, or null if the old value should be kept
     * @param name            the new name (not unique), or null if the old value should be kept
     * @param loginName       the new login name (unqiue over all mandators), or null if the old value should be kept
     * @param email           the new email, or null if the old value should be kept
     * @param isConfirmed     the new confirmed state, or null if the old value should be kept
     * @param isActive        the new active state, or null if the old value should be kept
     * @param validTo         the new valid to date, or null if the old value should be kept
     * @param validFrom       the new valid from date, or null if the old value should be kept
     * @param lang            the new language, or null if the old value should be kept
     * @param description     the new description, or null if the old value should be kept
     * @param allowMultiLogin true if the account may be active more than once at the same
     *                        time, may be null to keep the old value
     * @param contactDataId   id of the contact data
     * @throws FxEntryExistsException      if a user with the given login name already exists
     * @throws FxNoAccessException         if the caller lacks the permissions to update the user
     * @throws FxUpdateException           if the update failed
     * @throws FxNotFoundException         if the user to update does not exist
     * @throws FxInvalidParameterException if a parameter was invalid
     * @throws FxApplicationException      on errors
     */

    void update(long accountId, String password, Long defaultNode,
                String name, String loginName, String email, Boolean isConfirmed, Boolean isActive,
                Date validFrom, Date validTo, Long lang, String description,
                Boolean allowMultiLogin, Long contactDataId)
            throws FxApplicationException;

    /**
     * Updates some personal data of the specified user
     *
     * @param accountId the user to update the data for
     * @param password  the new password to assign
     * @param name      user name
     * @param loginName the new login name to assign
     * @param email     the new e-mail address to assign
     * @param lang      the new language to assign
     * @throws FxApplicationException on errors
     */
    void updateUser(long accountId, String password, String name, String loginName, String email, Long lang) throws FxApplicationException;

    /**
     * Updates some personal data of the specified user
     *
     * @param accountId    the user to update the data for
     * @param password     the new password to assign
     * @param hashPassword whether the password should be hashed (set to false for importing users, otherwise
     *                     this should always be true)
     * @param name         user name
     * @param loginName    the new login name to assign
     * @param email        the new e-mail address to assign
     * @param lang         the new language to assign
     * @throws FxApplicationException on errors
     * @since 3.2.0
     */
    void updateUser(long accountId, String password, boolean hashPassword, String name, String loginName, String email, Long lang) throws FxApplicationException;


    /**
     * Returns all users assigned to a group defined by its unique id.
     * <p/>
     * This function provides the parameters startIdx and maxEntries to allow a page-view of the users
     * in the GUI. This is neccesarry since a group may contain many thousands of users, which should
     * not be transfered at once to the client.<br>
     * The caller may only see groups of the mandator he belongs to plus GROUP_EVERYONE and GROUPE_PRIVATE.<br>
     * GLOBAL_SUPERVISOR may see the groups/users of all mandators.
     *
     * @param groupId    the group to get the users for
     * @param startIdx   the start index in the result, 0 based
     * @param maxEntries the maximum amount of users returned by the funktion (-1 for all), starting at startIdx
     * @return all users assigned to the given group
     * @throws FxApplicationException on errors
     * @throws FxNoAccessException    if the caller may not see the group
     * @throws FxLoadException        if the get failed
     * @throws FxNotFoundException    if the group does not exist
     */
    List<Account> getAssignedUsers(long groupId, int startIdx, int maxEntries)
            throws FxApplicationException;

    /**
     * Returns the amount of users within a group.
     *
     * @param groupId          the group to return the assignment count for
     * @param includeInvisible a group may contain users belonging to a foreign mandator, which are invisible
     *                         for the caller (except GLOBAL_SUPERVISOR). This parameter specifies wether to count those invisible
     *                         users or not.
     * @return the amount of users belonging to the group
     * @throws FxApplicationException on errors
     * @throws FxLoadException        if the load of the count failed
     */
    long getAssignedUsersCount(long groupId, boolean includeInvisible) throws FxApplicationException;

    /**
     * Retrieves all ACLs assigned to a given account.
     * <p/>
     * A empty resultset is returned if the account does not exist.<br>
     * A user may only see his own ACLAssignment.<br>
     * MANDATOR_FLEXIVE may retrive ACLAssignments for all his users.<br>
     * GLOBAL_SUPERVISOR may retrive the ACLAssignments of all users.
     *
     * @param accountId the user to get the ACLAssignments for
     * @return the ACLAssignments of the user
     * @throws FxLoadException        if the function failed to load the ACLAssignments
     * @throws FxNoAccessException    if the calling user may not access the ACLAssignment of the given user
     * @throws FxApplicationException on errors
     */
    List<ACLAssignment> loadAccountAssignments(long accountId) throws FxApplicationException;

    /**
     * Create contact data for all accounts that dont have them
     *
     * @throws FxApplicationException on errors
     */
    void fixContactData() throws FxApplicationException;

    /**
     * Generate a new REST API token, overwriting the previous one if it existed.
     *
     * @return  the API token
     * @throws FxApplicationException   on errors
     * @since 3.2.0
     */
    String generateRestToken() throws FxApplicationException;

    /**
     * Login using a REST API token. Currently this is implemented as a "light" login, i.e. the request ticket
     * will be set appropriately, but no "real" login is performed as the REST API calls don't have an associated
     * session.
     *
     * @param token    the REST API token
     * @throws FxApplicationException on errors
     * @throws FxRestApiTokenExpiredException when the token is expired or invalid (was replaced by a newer one)
     *
     * @since 3.2.0
     */
    void loginByRestToken(String token) throws FxApplicationException;
}
