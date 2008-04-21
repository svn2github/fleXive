/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation.
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
package com.flexive.ejb.beans;

import com.flexive.core.Database;
import static com.flexive.core.DatabaseConst.*;
import com.flexive.core.security.UserTicketStore;
import com.flexive.shared.*;
import com.flexive.shared.content.FxPermissionUtils;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.interfaces.*;
import com.flexive.shared.security.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.Resource;
import javax.ejb.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


/**
 * User Group management beans.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Stateless(name = "UserGroupEngine")
@TransactionManagement(TransactionManagementType.CONTAINER)
public class UserGroupEngineBean implements UserGroupEngine, UserGroupEngineLocal {
    private static transient Log LOG = LogFactory.getLog(UserGroupEngineBean.class);
    @Resource
    javax.ejb.SessionContext ctx;
    //@EJB AccountEngine account;
    @EJB
    SequencerEngineLocal seq;


    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public UserGroup load(long groupId) throws FxApplicationException {
        Connection con = null;
        Statement stmt = null;
        String sql = "SELECT MANDATOR,NAME,COLOR,AUTOMANDATOR,ISSYSTEM FROM " + TBL_GROUP + " WHERE ID=" + groupId;
        try {

            // Obtain a database connection
            con = Database.getDbConnection();

            // Create the new workflow instance
            stmt = con.createStatement();

            // Build statement
            ResultSet rs = stmt.executeQuery(sql);

            // Does the group exist at all?
            if (rs == null || !rs.next()) {
                FxNotFoundException nfe = new FxNotFoundException("ex.account.group.notFound.id", groupId);
                if (LOG.isInfoEnabled()) LOG.info(nfe);
                throw nfe;
            }
            long autoMandator = rs.getLong(4);
            if (rs.wasNull())
                autoMandator = -1;
            return new UserGroup(groupId, rs.getLong(1), autoMandator, rs.getBoolean(5), rs.getString(2), rs.getString(3));

        } catch (SQLException exc) {
            FxLoadException de = new FxLoadException(exc, "ex.usergroup.sqlError", exc.getMessage(), sql);
            LOG.error(de);
            throw de;
        } finally {
            Database.closeObjects(UserGroupEngineBean.class, con, stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public UserGroup loadMandatorGroup(long mandatorId) throws FxApplicationException {
        Connection con = null;
        Statement stmt = null;
        String sql = "SELECT ID,MANDATOR,NAME,COLOR,AUTOMANDATOR,ISSYSTEM FROM " + TBL_GROUP + " WHERE AUTOMANDATOR=" + mandatorId;

        try {

            // Obtain a database connection
            con = Database.getDbConnection();

            // Create the new workflow instance
            stmt = con.createStatement();

            // Build statement
            ResultSet rs = stmt.executeQuery(sql);

            // Does the group exist at all?
            if (rs == null || !rs.next()) 
                throw new FxNotFoundException("ex.account.group.notFound.id", mandatorId);
            long autoMandator = rs.getLong(5);
            if (rs.wasNull())
                autoMandator = -1;
            return new UserGroup(rs.getLong(1), rs.getLong(2), autoMandator, rs.getBoolean(6), rs.getString(3), rs.getString(4));
        } catch (SQLException exc) {
            FxLoadException de = new FxLoadException(exc, "ex.usergroup.sqlError", exc.getMessage(), sql);
            LOG.error(de);
            throw de;
        } finally {
            Database.closeObjects(UserGroupEngineBean.class, con, stmt);
        }
    }


    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public UserGroupList loadAll(long mandatorId) throws FxApplicationException {
        Connection con = null;
        Statement stmt = null;
        String sql = null;
        try {

            // Obtain a database connection
            con = Database.getDbConnection();

            // Create the new workflow instance
            stmt = con.createStatement();

            //            1  2        3    4     5            6
            sql = "SELECT ID,MANDATOR,NAME,COLOR,AUTOMANDATOR,ISSYSTEM FROM " + TBL_GROUP +
                    // Never display the dummy NULL group
                    " WHERE ID!=" + UserGroup.GROUP_NULL;
            if (mandatorId != -1)
                sql += " AND (MANDATOR=" + mandatorId + " or ID in (" +
                        UserGroup.GROUP_EVERYONE + "," + UserGroup.GROUP_OWNER + "))";
            sql += " ORDER BY MANDATOR,NAME";

            ResultSet rs = stmt.executeQuery(sql);

            // Process resultset
            ArrayList<UserGroup> aRes = new ArrayList<UserGroup>(100);
            while (rs != null && rs.next()) {
                long autoMandator = rs.getLong(5);
                if (rs.wasNull())
                    autoMandator = -1;
                aRes.add(new UserGroup(rs.getLong(1), rs.getLong(2), autoMandator, rs.getBoolean(6),
                        rs.getString(3), rs.getString(4)));
            }

            // Build result list
            UserGroupList res = new UserGroupList(aRes.toArray(new UserGroup[aRes.size()]));

            // Sanity check
            if (!res.contains(UserGroup.GROUP_EVERYONE) || !res.contains(UserGroup.GROUP_OWNER)) {
                FxLoadException le = new FxLoadException("ex.usergroup.oneOfSystemGroupsIsMissing");
                LOG.fatal(le);
                throw le;
            }

            return res;

        } catch (SQLException exc) {
            FxLoadException de = new FxLoadException(exc, "ex.usergroup.sqlError", exc.getMessage(), sql);
            LOG.error(de);
            throw de;
        } finally {
            Database.closeObjects(UserGroupEngineBean.class, con, stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public long create(String name, String color, long mandatorId) throws FxApplicationException {
        final UserTicket ticket = FxContext.get().getTicket();
        // Permission checks
        try {
            if (!ticket.isGlobalSupervisor()) {
                if (ticket.getMandatorId() != mandatorId) {
                    throw new FxNoAccessException("ex.usergroup.create.foreignMandator");
                }
                if (!ticket.isInRole(Role.MandatorSupervisor))
                    FxPermissionUtils.checkRole(ticket, Role.AccountManagement);
            }
        } catch (FxNoAccessException nae) {
            if (LOG.isInfoEnabled()) LOG.info(nae);
            throw nae;
        }

        Connection con = null;
        Statement stmt = null;
        PreparedStatement pstmt = null;
        String sql = null;
        try {

            // Sanity checks
            if (color == null || color.length() == 0) color = "#000000";
            if (color.charAt(0) != '#') color = "#" + color;
            IsValid(name, color);

            // Obtain a database connection
            con = Database.getDbConnection();

            // Obtain a new id
            long groupId = seq.getId(SequencerEngine.System.GROUP);

            // Create the new group
            sql = "INSERT INTO " + TBL_GROUP + " " +
                    "(ID,MANDATOR,AUTOMANDATOR,ISSYSTEM,NAME,COLOR,CREATED_BY,CREATED_AT,MODIFIED_BY,MODIFIED_AT) VALUES (" +
                    "?,?,NULL,FALSE,?,?,?,?,?,?)";
            final long NOW = System.currentTimeMillis();
            pstmt = con.prepareStatement(sql);
            pstmt.setLong(1, groupId);
            pstmt.setLong(2, mandatorId);
            pstmt.setString(3, name);
            pstmt.setString(4, color);
            pstmt.setLong(5, ticket.getUserId());
            pstmt.setLong(6, NOW);
            pstmt.setLong(7, ticket.getUserId());
            pstmt.setLong(8, NOW);
            pstmt.executeUpdate();

            // Return the new id
            return groupId;

        } catch (SQLException exc) {
            final boolean uniqueConstraintViolation = Database.isUniqueConstraintViolation(exc);
            ctx.setRollbackOnly();
            if (uniqueConstraintViolation) {
                FxEntryExistsException eee = new FxEntryExistsException("ex.usergroup.create.groupExists", name);
                if (LOG.isInfoEnabled()) LOG.info(eee);
                throw eee;
            } else {
                FxCreateException ce = new FxCreateException(exc, "ex.usergroup.sqlError", exc.getMessage(), sql);
                LOG.error(ce);
                throw ce;
            }
        } finally {
            Database.closeObjects(UserGroupEngineBean.class, null, pstmt);
            Database.closeObjects(UserGroupEngineBean.class, con, stmt);
        }
    }


    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void update(long groupId, String name, String color) throws FxApplicationException {

        final UserTicket ticket = FxContext.get().getTicket();

        // Load the group
        UserGroup aGroup;
        try {
            aGroup = load(groupId);
        } catch (FxLoadException exc) {
            throw new FxUpdateException(exc.getMessage(), exc);
        }

        // Permission checks
        checkPermission(aGroup, "ex.usergroup.noUpdatePerms");

        Connection con = null;
        Statement stmt = null;
        PreparedStatement pstmt;
        String sCurSql = null;
        try {

            // Sanity checks
            if (color != null && color.length() > 0 && color.charAt(0) != '#') {
                color = "#" + color;
            }
            IsValid(name, color);

            // Fill in new values
            if (name != null && color.length() > 0) aGroup.setName(name);
            if (color != null) aGroup.setColor(color);

            // Obtain a database connection
            con = Database.getDbConnection();

            // Create the new group
            sCurSql = "UPDATE " + TBL_GROUP + " SET " +
                    "NAME=?, COLOR=?, MODIFIED_BY=?, MODIFIED_AT=? " +
                    "WHERE ID=" + groupId;
            final long NOW = System.currentTimeMillis();
            pstmt = con.prepareStatement(sCurSql);
            pstmt.setString(1, aGroup.getName());
            pstmt.setString(2, aGroup.getColor());
            pstmt.setLong(3, ticket.getUserId());
            pstmt.setLong(4, NOW);
            pstmt.executeUpdate();
            pstmt.close();

        } catch (SQLException exc) {
            final boolean uniqueConstraintViolation = Database.isUniqueConstraintViolation(exc);
            ctx.setRollbackOnly();
            if (uniqueConstraintViolation) {
                FxEntryExistsException eee = new FxEntryExistsException("ex.usergroup.groupExists", name);
                if (LOG.isInfoEnabled()) LOG.info(eee);
                throw eee;
            } else {
                FxCreateException ce = new FxCreateException(exc, "ex.usergroup.sqlError", exc.getMessage(), sCurSql);
                LOG.error(ce);
                throw ce;
            }
        } finally {
            Database.closeObjects(UserGroupEngineBean.class, con, stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void remove(long groupId) throws FxApplicationException {

        // Check special groups
        if (groupId == UserGroup.GROUP_UNDEFINED) return;

        // Load the group
        UserGroup theGroup;
        try {
            theGroup = load(groupId);
        } catch (FxLoadException le) {
            throw new FxRemoveException(le.getMessage(), le);
        }

        // Check special groups
        if (theGroup.isSystem() && !FxContext.get().getRunAsSystem()) {
            FxNoAccessException nae = new FxNoAccessException("ex.usergroup.mayNotDeleteSystemGroups",
                    theGroup.getName());
            LOG.error(nae);
            throw nae;
        }

        // Caller may delete the group?
        checkPermission(theGroup, "ex.usergroup.noDeletePerms");

        Connection con = null;
        Statement stmt = null;
        String sCurSql;
        try {

            // Obtain a database connection
            con = Database.getDbConnection();

            // First of delete all user assignments to this group ..
            stmt = con.createStatement();
            sCurSql = "DELETE FROM " + TBL_ASSIGN_GROUPS + " WHERE USERGROUP=" + groupId;
            stmt.executeUpdate(sCurSql);
            stmt.close();

            // ... delete all roles assigned to this group
            stmt = con.createStatement();
            sCurSql = "DELETE FROM " + TBL_ASSIGN_ROLES + " WHERE USERGROUP=" + groupId;
            stmt.executeUpdate(sCurSql);
            stmt.close();

            // ... then delete all ACL assignments to this group ..
            stmt = con.createStatement();
            sCurSql = "DELETE FROM " + TBL_ASSIGN_ACLS + " WHERE USERGROUP=" + groupId;
            stmt.executeUpdate(sCurSql);
            stmt.close();

            // ... and finally delete the group itself;
            stmt = con.createStatement();
            sCurSql = "DELETE FROM " + TBL_GROUP + " WHERE ID=" + groupId;
            stmt.executeUpdate(sCurSql);

            // Update all active user tickets that are affected
            UserTicketStore.flagDirtyHavingGroupId(groupId);

            // Log
            if (LOG.isInfoEnabled()) LOG.info("Group [" + theGroup + "] was successfully deleted.");

        } catch (SQLException exc) {
            ctx.setRollbackOnly();
            FxRemoveException ce = new FxRemoveException(exc, "ex.usergroup.deleteSqlException", theGroup.getName());
            LOG.error(ce);
            throw ce;
        } finally {
            Database.closeObjects(UserGroupEngineBean.class, con, stmt);
        }


    }


    /**
     * Checks if the group name is valid.
     * Throws a FxInvalidParameterException if the name is invalid.
     *
     * @param sName  the name to check
     * @param sColor the color to check
     * @throws FxInvalidParameterException if the name is not valid
     */
    private static void IsValid(final String sName, String sColor) throws FxInvalidParameterException {
        if (sName == null || sName.length() == 0) {
            throw new FxInvalidParameterException("NAME", "ex.usergroup.nameEmpty");
        }
        if (sName.indexOf("'") > -1 || sName.indexOf("\"") > -1) {
            throw new FxInvalidParameterException("NAME", "ex.usergroup.nameContainsInvalidChars");
        }

        // Check the color
        if (sColor != null && !FxFormatUtils.isRGBCode(sColor)) {
            throw new FxInvalidParameterException("COLOR", "ex.usergroup.invalidColor");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setRoles(long groupId, List<Role> roles) throws FxApplicationException {
        long tmp[] = new long[roles == null ? 0 : roles.size()];
        if (roles != null) {
            int pos = 0;
            for (Role role : roles) {
                tmp[pos++] = role.getId();
            }
        }
        setRoles(groupId, tmp);
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void setRoles(long groupId, long[] roles)
            throws FxApplicationException {

        final UserTicket ticket = FxContext.get().getTicket();

        // EJBLookup the group
        UserGroup aGroup;
        try {
            aGroup = load(groupId);
        } catch (FxLoadException exc) {
            FxUpdateException ue = new FxUpdateException(exc.getMessage(), exc);
            if (LOG.isInfoEnabled()) LOG.info(ue);
            throw ue;
        }

        // Permission check
        if (!ticket.isGlobalSupervisor())
            try {
                if (aGroup.getMandatorId() != ticket.getMandatorId()) {
                    // foreign mandator
                    throw new FxNoAccessException("ex.usergroup.noPermSetRoles", aGroup.getName());
                }
                if (!(ticket.isInRole(Role.AccountManagement) || ticket.isInRole(Role.MandatorSupervisor))) {
                    throw new FxNoAccessException("ex.usergroup.noPermSetRoles", aGroup.getName());
                }
            } catch (FxNoAccessException nae) {
                if (LOG.isInfoEnabled()) LOG.info(nae);
                throw nae;
            }

        // Bye bye duplicates
        roles = FxArrayUtils.removeDuplicates(roles);
        //only allow to assign roles which the calling user is a member of (unless it is a global supervisor)
        if (!ticket.isGlobalSupervisor()) {
            Role[] orgRoles = getRoles(groupId).getRoles();
            long[] orgRoleIds = new long[orgRoles.length];
            for (int i = 0; i < orgRoles.length; i++)
                orgRoleIds[i] = orgRoles[i].getId();
            //check removed roles
            for (long check : orgRoleIds) {
                if (!FxArrayUtils.containsElement(roles, check)) {
                    if (!ticket.isInRole(Role.getById(check))) {
                        ctx.setRollbackOnly();
                        throw new FxNoAccessException("ex.account.roles.assign.noMember.remove", Role.getById(check).getName());
                    }
                }
            }
            //check added roles
            for (long check : roles) {
                if (!FxArrayUtils.containsElement(orgRoleIds, check)) {
                    if (!ticket.isInRole(Role.getById(check))) {
                        ctx.setRollbackOnly();
                        throw new FxNoAccessException("ex.account.roles.assign.noMember.add", Role.getById(check).getName());
                    }
                }
            }
        }

        // Write roles to database
        Connection con = null;
        Statement stmt = null;
        String sCurSql;

        try {
            // Obtain a database connection
            con = Database.getDbConnection();

            // Delete the old assignments of the user
            sCurSql = "DELETE FROM " + TBL_ASSIGN_ROLES + " WHERE USERGROUP=" + groupId;
            stmt = con.createStatement();
            stmt.executeUpdate(sCurSql);
            stmt.close();

            // Store the new assignments of the user
            for (long role : roles) {

                if (Role.isUndefined(role)) continue;

                stmt = con.createStatement();
                sCurSql = "INSERT INTO " + TBL_ASSIGN_ROLES +
                        " (ACCOUNT,USERGROUP,ROLE) VALUES (" + Account.NULL_ACCOUNT + "," + groupId + "," + role + ")";
                stmt.executeUpdate(sCurSql);
                stmt.close();
            }

            // Update all active user tickets that are affected
            UserTicketStore.flagDirtyHavingGroupId(groupId);

        } catch (SQLException exc) {
            ctx.setRollbackOnly();
            FxUpdateException dbe = new FxUpdateException(exc, "ex.usergroup.updateRolesSqlException", aGroup.getName());
            LOG.error(dbe);
            throw dbe;
        } finally {
            Database.closeObjects(UserGroupEngineBean.class, con, stmt);
        }


    }

    /**
     * Returns true if the caller may see the group and its roles and assignments.
     *
     * @param grp the group
     * @return true if the caller may see the group its roles and assignments
     */
    public boolean mayAccessGroup(UserGroup grp) {
        final UserTicket ticket = FxContext.get().getTicket();
        return ticket.isGlobalSupervisor() || grp.getId() == UserGroup.GROUP_EVERYONE ||
                grp.getId() == UserGroup.GROUP_OWNER || grp.getMandatorId() == ticket.getMandatorId();
    }


    /**
     * Checks if the caller may update/edit/delete a given group.
     *
     * @param group the group to check for
     * @param mode  mode displayed in the error message, eg 'update', 'delete', ..
     * @throws FxNoAccessException if the caller lacks the permissions
     */
    private static void checkPermission(UserGroup group, String mode) throws FxNoAccessException {
        final UserTicket ticket = FxContext.get().getTicket();
        // Permission checks
        try {
            if (!ticket.isGlobalSupervisor()) {
                if (ticket.getMandatorId() != group.getMandatorId()) {
                    throw new FxNoAccessException(mode);
                }
                if (!ticket.isInRole(Role.MandatorSupervisor))
                    FxPermissionUtils.checkRole(ticket, Role.AccountManagement);
            }
        } catch (FxNoAccessException nae) {
            if (LOG.isInfoEnabled()) LOG.info(nae);
            throw nae;
        }
    }


    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public RoleList getRoles(long groupId) throws FxApplicationException {
        Connection con = null;
        Statement stmt = null;
        final String sql = "SELECT DISTINCT ROLE FROM " + TBL_ASSIGN_ROLES + " WHERE USERGROUP=" + groupId;

        // EJBLookup the group
        final UserGroup aGroup = load(groupId);

        // Permission check
        if (!mayAccessGroup(aGroup)) {
            FxNoAccessException nae = new FxNoAccessException("ex.usergroup.noPermissionsToReadRoles", aGroup.getName());
            if (LOG.isInfoEnabled()) LOG.info(nae);
            throw nae;
        }

        try {
            // Obtain a database connection
            con = Database.getDbConnection();
            // Load the roles
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            RoleList result = new RoleList();
            while (rs != null && rs.next()) {
                result.add(rs.getByte(1));
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Roles for group [" + groupId + "]: " + result.toNameArray());
            }
            return result;
        } catch (SQLException exc) {
            FxLoadException le = new FxLoadException(exc, "ex.usergroup.sqlError", exc.getMessage(), sql);
            LOG.error(le);
            throw le;
        } finally {
            Database.closeObjects(UserGroupEngineBean.class, con, stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void rebuildMandatorGroups() throws FxApplicationException {
        Connection con = null;
        PreparedStatement ps = null;
        Statement stmt = null;
        String sql = "";
        try {
            con = Database.getDbConnection();
            stmt = con.createStatement();
            List<Mandator> missing = new ArrayList<Mandator>(5);
            for (Mandator m : CacheAdmin.getEnvironment().getMandators(true, true)) {
                try {
                    UserGroup g = loadMandatorGroup(m.getId());
                    sql = "DELETE FROM " + TBL_ASSIGN_GROUPS + " WHERE USERGROUP=" + g.getId();
                    stmt.executeUpdate(sql);
                    sql = "INSERT INTO " + TBL_ASSIGN_GROUPS + " (ACCOUNT,USERGROUP) (SELECT a.id, " + g.getId() +
                            " FROM " + TBL_ACCOUNTS + " a WHERE a.MANDATOR=" + m.getId() + " AND a.ID!=" + Account.NULL_ACCOUNT + ")";
                    stmt.executeUpdate(sql);
                } catch (FxNotFoundException e) {
                    missing.add(m);
                }
            }
            final long NOW = System.currentTimeMillis();
            for (Mandator m : missing) {
                sql = "INSERT INTO " + TBL_GROUP + " " +
                        "(ID,MANDATOR,AUTOMANDATOR,ISSYSTEM,NAME,COLOR,CREATED_BY,CREATED_AT,MODIFIED_BY,MODIFIED_AT) VALUES (" +
                        "?,?," + m.getId() + ",TRUE,?,?,?,?,?,?)";
                if (ps == null)
                    ps = con.prepareStatement(sql);
                long gid = seq.getId(SequencerEngine.System.GROUP);
                ps.setLong(1, gid);
                ps.setLong(2, m.getId());
                ps.setString(3, "Everyone (" + m.getName() + ")");
                ps.setString(4, "#00AA00");
                ps.setLong(5, 0);
                ps.setLong(6, NOW);
                ps.setLong(7, 0);
                ps.setLong(8, NOW);
                ps.executeUpdate();
                sql = "INSERT INTO " + TBL_ASSIGN_GROUPS + " (ACCOUNT,USERGROUP) (SELECT a.ID, " + gid +
                        " FROM " + TBL_ACCOUNTS + " a WHERE a.MANDATOR=" + m.getId() + " AND a.ID!=" + Account.NULL_ACCOUNT + ")";
                stmt.executeUpdate(sql);
            }
        } catch (SQLException exc) {
            FxLoadException le = new FxLoadException(exc, "ex.usergroup.sqlError", exc.getMessage(), sql);
            LOG.error(le);
            throw le;
        } finally {
            if (ps != null)
                try {
                    ps.close();
                } catch (SQLException e) {
                    //ignore
                }
            Database.closeObjects(UserGroupEngineBean.class, con, stmt);
        }
    }

}
