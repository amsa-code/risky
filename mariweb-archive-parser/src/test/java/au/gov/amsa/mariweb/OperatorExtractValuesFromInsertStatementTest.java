package au.gov.amsa.mariweb;

import static au.gov.amsa.mariweb.OperatorExtractValuesFromInsertStatement.parseValuesFromClause;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import rx.Subscriber;

public class OperatorExtractValuesFromInsertStatementTest {

    @Test
    public void testParseValuesFromClause() {
        MySubscriber s = createSubscriber();
        parseValuesFromClause("(1,2,3,4),(5,6)", s);
        assertEquals(2, s.list().size());
        assertEquals(Arrays.asList("1", "2", "3", "4"), s.list().get(0));
        assertEquals(Arrays.asList("5", "6"), s.list().get(1));
    }

    @Test
    public void testParseValuesFromClauseWithQuotes() {
        MySubscriber s = createSubscriber();
        parseValuesFromClause("(1,'ab')", s);
        assertEquals(Arrays.asList("1", "ab"), s.latest());
    }

    @Test
    public void testParseValuesFromClauseWithDoubleQuote() {
        MySubscriber s = createSubscriber();
        parseValuesFromClause("('ab\\\"')", s);
        assertEquals(Arrays.asList("ab\""), s.latest());
    }

    @Test
    public void testParseValuesFromClauseWithEscapedBackslash() {
        MySubscriber s = createSubscriber();
        parseValuesFromClause("('\\\\i')", s);
        assertEquals(Arrays.asList("\\i"), s.latest());
    }

    @Test
    public void testParseValuesFromClauseWithEscapedQuote() {
        MySubscriber s = createSubscriber();
        parseValuesFromClause("('\\'')", s);
        assertEquals(Arrays.asList("'"), s.latest());
    }

    @Test
    public void testParseValuesFromClauseWithBackQuote() {
        MySubscriber s = createSubscriber();
        parseValuesFromClause("('`00003vP10,4*58')", s);
        assertEquals(Arrays.asList("`00003vP10,4*58"), s.latest());
    }

    @Test
    public void testParseValuesFromClauseWithEscapedQuotes() {
        MySubscriber s = createSubscriber();
        parseValuesFromClause("('\\'\\'')", s);
        assertEquals(Arrays.asList("''"), s.latest());
    }

    @Test
    public void testParseValuesFromClauseWithEscapedQuotesInterleavedWithCharacters() {
        MySubscriber s = createSubscriber();
        parseValuesFromClause("('a\\'b\\'c')", s);
        assertEquals(Arrays.asList("a'b'c"), s.latest());
    }

    @Test
    public void testParseValuesFromClauseWithQuotesAndCommasBetweenQuotes() {
        MySubscriber s = createSubscriber();
        parseValuesFromClause("(1,'ab,c')", s);
        assertEquals(Arrays.asList("1", "ab,c"), s.latest());
    }

    @Test
    public void testParseValuesFromClauseWithTwoQuotedStrings() {
        MySubscriber s = createSubscriber();
        parseValuesFromClause("('ab','cd')", s);
        assertEquals(Arrays.asList("ab", "cd"), s.latest());
    }

    public MySubscriber createSubscriber() {
        return new MySubscriber();
    }

    private static class MySubscriber extends Subscriber<List<String>> {

        private final List<List<String>> list = new ArrayList<List<String>>();

        @Override
        public void onCompleted() {
            // TODO Auto-generated method stub

        }

        @Override
        public void onError(Throwable e) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onNext(List<String> t) {
            list.add(t);
        }

        List<String> latest() {
            return list.get(list.size() - 1);
        }

        List<List<String>> list() {
            return list;
        }

    };

}
