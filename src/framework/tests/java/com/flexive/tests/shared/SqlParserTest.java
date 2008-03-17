package com.flexive.tests.shared;

import org.testng.annotations.Test;
import org.apache.commons.lang.StringUtils;
import com.flexive.sqlParser.*;
import com.flexive.shared.search.query.PropertyValueComparator;
import com.flexive.shared.search.query.VersionFilter;
import com.flexive.shared.FxFormatUtils;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.structure.FxSelectListItem;
import com.flexive.shared.structure.FxSelectListEdit;
import com.flexive.shared.structure.FxSelectList;
import com.flexive.shared.value.*;

import java.util.Date;
import java.util.Arrays;

/**
 * Tests for the FxSQL parser. Checks various syntax features, but does not
 * execute real queries.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public class SqlParserTest {

    @Test(groups = {"shared", "search"})
    public void emptyWhereClause() throws SqlParserException {
        parse("SELECT co.id FROM content co", new String[]{"co.id"});
        try {
            parse("SELECT co.id FROM content co WHERE", new String[]{"co.id"});
            assert false : "WHERE specified, but no conditions - expected failure";
        } catch (SqlParserException e) {
            // pass
        }
    }

    @Test(groups = {"shared", "search"})
    public void contentTypeFilter() throws SqlParserException {
        final FxStatement stmt1 = parse("SELECT co.id FROM content co FILTER co.TYPE=21", new String[]{"co.id"});
        assert stmt1.getContentTypeFilter().equals("21") : "Content type filter was " + stmt1.getContentTypeFilter() + ", expected: 21";

        final FxStatement stmt2 = parse("SELECT co.id FROM content co FILTER co.TYPE=mytype", new String[]{"co.id"});
        assert stmt2.getContentTypeFilter().equalsIgnoreCase("mytype") : "Content type filter was " + stmt2.getContentTypeFilter() + ", expected: mytype";
    }

    @Test(groups = {"shared", "search"})
    public void versionFilter() throws SqlParserException {
        assert parse("SELECT co.id FROM content co FILTER co.VERSION=max").getVersionFilter().equals(VersionFilter.MAX);
        assert parse("SELECT co.id FROM content co FILTER co.VERSION=LIVE").getVersionFilter().equals(VersionFilter.LIVE);
        assert parse("SELECT co.id FROM content co FILTER co.VERSION=ALL").getVersionFilter().equals(VersionFilter.ALL);
        // auto gets the version through some user session magic, but it should definitely not return auto
        assert !parse("SELECT co.id FROM content co FILTER co.VERSION=AUTO").getVersionFilter().equals(VersionFilter.AUTO);
        try {
            parse("SELECT co.id FROM content co FILTER co.VERSION=15");
            assert false : "Specific versions cannot be selected.";
        } catch (SqlParserException e) {
            // pass
        }
    }

    @Test(groups = {"shared", "search"})
    public void ignoreCaseFilter() throws SqlParserException {
        assert parse("SELECT co.id FROM content co FILTER IGNORE_CASE=T").getIgnoreCase();
        assert parse("SELECT co.id FROM content co FILTER IGNORE_CASE=t").getIgnoreCase();
        assert parse("SELECT co.id FROM content co FILTER IGNORE_CASE=true").getIgnoreCase();
        assert !parse("SELECT co.id FROM content co FILTER IGNORE_CASE=F").getIgnoreCase();
        assert !parse("SELECT co.id FROM content co FILTER IGNORE_CASE=f").getIgnoreCase();
        assert !parse("SELECT co.id FROM content co FILTER IGNORE_CASE=false").getIgnoreCase();
    }

    @Test(groups = {"shared", "search"})
    public void maxResultRowsFilter() throws SqlParserException {
        assert parse("SELECT co.id FROM content co FILTER MAX_RESULTROWS=21").getMaxResultRows() == 21;
        assert parse("SELECT co.id FROM content co FILTER MAX_RESULTROWS=0").getMaxResultRows() == 0;
        try {
            parse("SELECT co.id FROM content co FILTER MAX_RESULTROWS=-1");
            assert false : "Negative values should not be allowed for filter value MAX_RESULTROWS.";
        } catch (SqlParserException e) {
            // pass
        }
    }

    @Test(groups = {"shared", "search"})
    public void searchLanguagesFilter() throws SqlParserException {
        assert parse("SELECT co.id FROM content co").getTableByAlias("co").getSearchLanguages().length == 0;
        assert parse("SELECT co.id FROM content co FILTER co.SEARCH_LANGUAGES=de").getTableByAlias("co").getSearchLanguages()[0].equals("de");
        assert parse("SELECT co.id FROM content co FILTER co.SEARCH_LANGUAGES=de|en").getTableByAlias("co").getSearchLanguages()[0].equals("de");
        assert parse("SELECT co.id FROM content co FILTER co.SEARCH_LANGUAGES=de|en").getTableByAlias("co").getSearchLanguages()[1].equals("en");
        try {
            parse("SELECT co.id FROM content co FILTER SEARCH_LANGUAGES=de");
            assert false : "Filter SEARCH_LANGUAGES illegally specified without a table alias.";
        } catch (SqlParserException e) {
            // pass
        }
    }

    @Test(groups = {"shared", "search"})
    public void briefcaseFilter() throws SqlParserException {
        assert parse("SELECT co.id FROM content co").getBriefcaseFilter().length == 0;
        assert parse("SELECT co.id FROM content co FILTER briefcase=1").getBriefcaseFilter()[0] == 1;
        assert parse("SELECT co.id FROM content co FILTER briefcase=1|21").getBriefcaseFilter()[0] == 1;
        assert parse("SELECT co.id FROM content co FILTER briefcase=1|21").getBriefcaseFilter()[1] == 21;
    }

    @Test(groups = {"shared", "search"})
    public void combinedFilters() throws SqlParserException {
        final FxStatement stmt = parse("SELECT co.id FROM content co \n" +
                "FILTER IGNORE_CASE=false, max_resultrows=21, co.SEARCH_LANGUAGES=fr|it, briefcase=2|3,\n" +
                "       co.version=max, co.type=mine\n");
        assert !stmt.getIgnoreCase();
        assert stmt.getMaxResultRows() == 21;
        assert stmt.getTableByAlias("co").getSearchLanguages()[0].equals("fr");
        assert stmt.getTableByAlias("co").getSearchLanguages()[1].equals("it");
        assert stmt.getBriefcaseFilter()[0] == 2;
        assert stmt.getBriefcaseFilter()[1] == 3;
    }

    @Test(groups = {"shared", "search"})
    public void basicConditionComparators() throws SqlParserException {
        for (PropertyValueComparator comp : PropertyValueComparator.values()) {
            final String query = "SELECT co.id FROM content co WHERE " + comp.getSql("co.property", "myvalue");
            try {
                parse(query);
            } catch (SqlParserException e) {
                assert false : "Failed to submit query with comparator " + comp + ":\n"
                        + query + "\n\nError message: " + e.getMessage();
            }
        }
    }

    @Test(groups = {"shared", "search"})
    public void nestedConditionComparators() {
        for (PropertyValueComparator comp : PropertyValueComparator.values()) {
            final Date date = new Date();
            final String query = "SELECT co.id FROM content co WHERE " + comp.getSql("co.p1", 1) + " AND ("
                    + comp.getSql("co.p2", "stringval") + " OR ("
                    + comp.getSql("co.p3", 2) + " AND " + comp.getSql("co.p4", date)
                    + "))";
            try {
                final FxStatement stmt = parse(query);

                // check root expression
                final Brace root = stmt.getRootBrace();
                assert root.isAnd() : "Root expression should be AND";
                assert root.getElements().length == 2 : "Root should have two children, has: " + Arrays.asList(root.getElements());
                checkStatementCondition(root.getElementAt(0), comp, "co.p1", "1");

                // check first nested level
                final Brace level1 = (Brace) root.getElementAt(1);
                assert level1.isOr() : "Level 1 expression should be 'or'";
                assert level1.getElements().length == 2 : "Level1 should have two children, has: " + Arrays.asList(level1.getElements());
                checkStatementCondition(level1.getElementAt(0), comp, "co.p2", FxFormatUtils.escapeForSql("stringval"));

                // check innermost level
                final Brace level2 = (Brace) level1.getElementAt(1);
                assert level2.isAnd() : "Level 2 expression should be 'and'";
                assert level2.getElements().length == 2 : "Level2 should have two children, has: " + Arrays.asList(level2.getElements());
                checkStatementCondition(level2.getElementAt(0), comp, "co.p3", "2");
                checkStatementCondition(level2.getElementAt(1), comp, "co.p4", FxFormatUtils.escapeForSql(date));
            } catch (Exception e) {
                assert false : "Failed to submit query with comparator " + comp + ":\n"
                        + query + "\n\nError message: " + e.getMessage();
            }
        }
    }

    @Test(groups = {"shared", "search"})
    public void dataTypeSupport() throws SqlParserException {
        final FxSelectListEdit selectList = new FxSelectList("test").asEditable();
        final FxSelectListItem item1 = new FxSelectListItem(25, selectList, -1, new FxString("label1"));
        final FxSelectListItem item2 = new FxSelectListItem(28, selectList, -1, new FxString("label2"));
        final FxValue[] testData = {
                new FxString("som'e test string"),
                new FxHTML("<h1>\"HTML c'aption\"</h1>"),
                new FxFloat(1.21f),
                new FxDouble(1.21),
                new FxDate(new Date()),
                new FxSelectOne(item1),
                new FxSelectMany(new SelectMany(selectList).select(item1.getId()).select(item2.getId())),
                new FxBoolean(false),
                new FxBoolean(true),
                new FxLargeNumber(2741824312312L),
                new FxNumber(21),
                new FxReference(new ReferencedContent(21, FxPK.MAX)),
                new FxReference(new ReferencedContent(21, FxPK.LIVE)),
                new FxReference(new ReferencedContent(21, 4))
        };
        for (FxValue value : testData) {
            final FxStatement stmt = parse("SELECT co.id FROM content co WHERE co.value = " + FxFormatUtils.escapeForSql(value));
            final Object conditionValue = ((Condition) stmt.getRootBrace().getElementAt(0)).getRValueInfo().getValue();
            assert StringUtils.isNotBlank(value.getSqlValue());
            assert conditionValue.equals(value.getSqlValue()) : "SQL condition value should be " + value.getSqlValue() + ", is: " + conditionValue;
        }
    }

    @Test(groups = {"shared", "search"})
    public void selectFunctions() throws SqlParserException {
        assert parse("SELECT min(co.id) FROM content co").getSelectedValues().get(0)
                .getValue().getFunctions()[0].equals("min");
        final Value val1 = parse("SELECT co.id, min(max(avg(co.id))) FROM content co").getSelectedValues().get(1).getValue();
        assert Arrays.equals(val1.getFunctions(), new String[]{"min", "max", "avg"})
                : "Expected functions min, max, avg; got: " + Arrays.asList(val1.getFunctions());
    }

    @Test(groups = {"shared", "search"})
    public void orderBy() throws SqlParserException {
        for (String valid : new String[]{
                "SELECT co.id FROM content co ORDER BY co.id, 1",
                "SELECT co.id FROM content co ORDER BY co.id ASC",
                "SELECT co.id FROM content co ORDER BY co.id",
                "SELECT co.id FROM content co ORDER BY 1",
                "SELECT co.id FROM content co ORDER BY 1 ASC, 1 DESC",
                "SELECT co.id FROM content co ORDER BY 1 DESC, 1 DESC"
        }) {
            final FxStatement stmt = parse(valid);
            assert stmt.getOrderByValues().get(0).getColumnIndex() == 0
                    : "Order by column index should be 0, was: " + stmt.getOrderByValues().get(0).getColumnIndex();
        }
        // some invalid queries
        for (String invalid : new String[]{
                "SELECT co.id FROM content co ORDER BY co.id2", // order by value not selected
                "SELECT co.id, co.ver FROM content co ORDER BY 3",
                "SELECT co.id, co.ver FROM content co ORDER BY co.1",
                "SELECT co.id, co.ver FROM content co ORDER BY co.id DESCC",
        }) {
            try {
                parse(invalid);
                assert false : "Query " + invalid + " is invalid.";
            } catch (SqlParserException e) {
                // pass
            }
        }
    }

    @Test(groups = {"shared", "search"})
    public void groupBy() throws SqlParserException {
        for (String valid : new String[]{
                "SELECT co.id FROM content co GROUP BY co.id",
                "SELECT co.id FROM content co GROUP BY co.id, co.id",
                "SELECT co.id FROM content co GROUP BY 1",
        }) {
            parse(valid);
            // TODO: add checks, since group by are recognized by the parser, but not evaluated
        }

        /*for (String invalid : new String[] {
                "SELECT co.id FROM content co GROUP BY co.id2",
                "SELECT co.id FROM content co GROUP BY 2",
        }) {
            try {
                parse(invalid);
                assert false : "Query " + invalid + " is invalid.";
            } catch (SqlParserException e) {
                // pass
            }
        }*/
    }

    @Test(groups = {"shared", "search"})
    public void queryComments() throws SqlParserException {
        parse("SELECT co.ID /* some comment */ FROM content -- line-comment\nco");
        parse("SELECT co.ID /* some \n multiline -- nested \n comment */ FROM content co");
        parse("SELECT co.ID FROM content co WHERE co.property /* some comment */ = /* some comment */ 21");
        parse("SELECT /* some \ncomment */ co.ID FROM /* some \n\ncomment */ content co -- another comment");
    }

    private void checkStatementCondition(BraceElement element, PropertyValueComparator comp, String lvalue, String rvalue) {
        assert element instanceof Condition : "First root child should be a condition, is: " + element;
        final Condition condition = (Condition) element;
        assert condition.getLValueInfo().getValue().equals(lvalue);
        assert !comp.isNeedsInput() || !condition.getRValueInfo().isNull();
        assert !comp.isNeedsInput() || condition.getRValueInfo().getValue().equals(rvalue)
                : "RValue should be " + rvalue + ", is: " + condition.getRValueInfo().getValue();
        assert !comp.isNeedsInput() || condition.getConstant().getValue().equals(rvalue);
    }

    /**
     * Parses the given query, performs basic validity checks based on the additional parameters
     * like selected columns, and returns the parsed statement.
     *
     * @param query           the FxSQL query
     * @param selectedColumns the selected columns, the returned statement will be checked to contain these columns
     * @return the parsed FxSQL statement
     * @throws com.flexive.sqlParser.SqlParserException
     *          on parser errors
     */
    private FxStatement parse(String query, String[] selectedColumns) throws SqlParserException {
        final FxStatement stmt2 = FxStatement.parseSql(query);
        checkStatement(stmt2, selectedColumns);
        return stmt2;
    }

    /**
     * Parses the given query, performs basic validity checks and returns the parsed statement.
     *
     * @param query the FxSQL query
     * @return the parsed FxSQL statement
     * @throws SqlParserException on parser errors
     */
    private FxStatement parse(String query) throws SqlParserException {
        return parse(query, null);
    }


    /**
     * Perform basic validity checks of common queries.
     *
     * @param statement       the statement to be checked
     * @param selectedColumns the selected column(s). If null, the corresponding tests will be skipped.
     * @return the statement
     */
    private FxStatement checkStatement(FxStatement statement, String[] selectedColumns) {
        assert statement.getTables().length == 1 : "One table should be selected, got: " + statement.getTables().length;
        assert statement.getTableByType(Table.TYPE.CONTENT) != null : "No content table selected";
        assert statement.getParserExecutionTime() >= 0 : "Parser execution time not set.";
        assert statement.getTableByAlias("co") != null;
        assert statement.getTableByAlias("co").getType().equals(Table.TYPE.CONTENT);
        if (selectedColumns != null) {
            for (int i = 0; i < statement.getSelectedValues().size(); i++) {
                final SelectedValue value = statement.getSelectedValues().get(i);
                assert value.getAlias().equals(selectedColumns[i]) : "Unexpected column selected: " + value;
            }
        }
        return statement;
    }
}
