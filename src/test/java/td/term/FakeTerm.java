package td.term;

record FakeTerm(String key, String title, TermText sentence) implements Term {

    @Override
    public TermText describe() {
        return this.sentence;
    }
}
