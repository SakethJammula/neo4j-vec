//package regpattern2vec;
//
//import org.neo4j.graphdb.*;
//import org.neo4j.logging.Log;
//import org.neo4j.procedure.Context;
//import org.neo4j.procedure.Description;
//import org.neo4j.procedure.Name;
//import org.neo4j.procedure.Procedure;
//
//import java.util.*;
//import java.util.stream.Stream;
//
//public class GetRegPatternWalksIter {
//
//    @Context
//    public Log log;
//
//    @Context
//    public Transaction transaction;
//
//    @Procedure(value = "regpattern2vec.GetRegPatternWalksIter")
//    @Description("Generates random walks based on regular pattern.")
//    public Stream<Walks> getRegPatternWalks(
//            @Name("pattern") String pattern,
//            @Name("walks") Long walks,
//            @Name("length") Long walkLength
//    ) {
//        Long walksNumber = walks;
//        Long walkLengthInt = walkLength;
//
//        log.info("Given input: " + pattern);
//
//        // (Author)(Paper)*(Conference)
//
//        String dfaUsed = "(q0,Author->q1),(q1,Paper->q1|Conference->q2)";
//
//        List<String> walkLabels = new ArrayList<>();
//        walkLabels.add("Author");
//        walkLabels.add("Paper");
//        walkLabels.add("Conference");
//        // Used walkLabels to get "Author" for findNodes later in the code.
//        // Need to get the first input symbol from dfaUsed.
//
//        Map<String, Map<String, String>> transitionFunc = computeTransitionFunction(dfaUsed);
//        //{q0={Author=q1}, q1={Paper=q1, Conference=q2}, q2={Paper=q1, Conference=q2}}
//        // Use two dimensional arrays (0,1,2)
//        Walks wks = new Walks();
//        wks.walks = new ArrayList<>();
//
//        transaction.findNodes(Label.label(walkLabels.get(0)))
//                .forEachRemaining(startNode -> {
//                    for (int i = 0; i < walksNumber; i++) {
//                        wks.walks.add(
//                                getRegPatternWalk(startNode, walkLengthInt, transitionFunc)
//
//                        );
//                    }
//                });
//
//        return Stream.of(wks);
//    }
//    public static Map<String, Map<String, String>> computeTransitionFunction(String dfaUsed) {
//        Map<String, Map<String, String>> transitionFunc = new HashMap<>();
//        String[] transitions = dfaUsed.split("\\),\\(");
//        for (String transition : transitions) {
//            transition = transition.replaceAll("\\(|\\)", "");
//            String[] parts = transition.split(",");
//            String state = parts[0];
//            String[] inputs = parts[1].split("\\|");
//            for (String input : inputs) {
//                String[] pair = input.split("->");
//                String symbol = pair[0];
//                String next = pair[1];
//                if (!transitionFunc.containsKey(state)) {
//                    transitionFunc.put(state, new HashMap<>());
//                }
//                transitionFunc.get(state).put(symbol, next);
//            }
//        }
//        return transitionFunc;
//    }
//
//    private String getRegPatternWalk(Node currentNode, Long remainingSteps, Map<String, Map<String, String>> transitionFunc) {
//        // {q0={Author=q1}, q1={Paper=q1, Conference=q2}, q2={Paper=q1, Conference=q2}}
//        String propertyKey = "";
//        if (remainingSteps == 0) {
//            propertyKey = currentNode.getLabels().iterator().next().name() + "_" + currentNode.getId();
//            return propertyKey;
//        }
//        log.info("Current Node: " + currentNode.toString());
//        log.info("Current Labels from getLabels(): " + currentNode.getLabels());
//        log.info("Current Node Iterator: " + currentNode.getLabels().iterator());
//        log.info("Current Labels from getLabels() next : " + currentNode.getLabels().iterator().next());
//        String currentLabel = currentNode.getLabels().iterator().next().name();
//        log.info("Current Label: " + currentLabel);
//        // {q0={Author=q1}, q1={Paper=q1, Conference=q2}, q2={Paper=q1, Conference=q2}}
//
//        String internalKey = currentLabel;
//        Set<String> externalKeys = new HashSet<>();
//
//        for (Map.Entry<String, Map<String, String>> entry : transitionFunc.entrySet()) {
//            if (entry.getValue().containsKey(internalKey)) {
//                externalKeys.add(entry.getKey());
//            }
//        }
//        String firstExternalKey = externalKeys.iterator().next();
//        log.info("First External Key:" + firstExternalKey);
//
//        Map<String, String> currentTransitions = transitionFunc.get(firstExternalKey);
//        // Map<String, String> currentTransitions = transitionFunc.get(transitionFunc.keySet().iterator().next());
//        //currentTransitions = {Author = q1}
//        for (Map.Entry<String, String> entry : currentTransitions.entrySet()) {
//            String key = entry.getKey();
//            String value = entry.getValue();
//            log.info(key + " => " + value);
//        }
//
//        if (currentTransitions == null || currentTransitions.isEmpty()) {
//            // Reached a dead end, stop the walk
//            propertyKey = currentNode.getLabels().iterator().next().name() + "_" + currentNode.getId();
//            log.info("In if loop: " + propertyKey);
//            return propertyKey;
//        }
//
//        // Choose the next label based on the transition probabilities
//        String chosenLabel = chooseLabel(currentTransitions);
//        log.info("Chosen Label: " + chosenLabel);
//        // chosenLabel = "Author"
//
//        // Choose the next node with the chosen label
//        Node chosenNode = chooseNode(currentNode, chosenLabel, currentTransitions);
//        // Node chosenNode = chooseNode(Author_0, Author, {Author=q1}}
//        log.info("Chosen Node: " + chosenNode);
//
//        // Recursively continue the walk
//        String propertyVal = chosenNode.getLabels().iterator().next().name() + "_" + chosenNode.getId();
//        log.info("PropertyVal: " + propertyVal);
//        return propertyVal + " " + getRegPatternWalk(chosenNode, remainingSteps - 1, transitionFunc);
//    }
//
//    private String chooseLabel(Map<String, String> currentTransitions) {
//        // currentTransitions = {Author = q1}
//        double totalWeight = 5.0;
//        log.info("totalWeight" + totalWeight);
//        // Generate a random number between 0 and the total weight
//        double randomValue = Math.random() * totalWeight;
//        log.info("randomValue" + randomValue);
//
//        // Choose the label based on the random value and the transition probabilities
//        double cumulativeWeight = 0.2;
//
//        for (Map.Entry<String, String> transition : currentTransitions.entrySet()) {
//            if (randomValue <= cumulativeWeight) {
//                log.info("transition.getKey():" + transition.getKey());
//                // Author
//                return transition.getKey();
//            }
//        }
//
//        // Should never reach this point, but return the first label as a fallback
//        return currentTransitions.keySet().iterator().next();
//    }
//    private Node chooseNode(Node currentNode, String chosenLabel, Map<String, String> transitionFunc) {
//        log.info("Entered chooseNode:");
//        // chooseNode(Author_0, Author, {q0={Author=q1}, q1={Paper=q1, Conference=q2}, q2={Paper=q1, Conference=q2}}
//        log.info(currentNode.getLabels().toString()); // Author
//        String currentLabel = currentNode.getLabels().iterator().next().name();
//        log.info("Current Label:" + currentLabel); // Author
//
//        //   Map<String, String> currentTransitionsBefore = transitionFunc.get("Author");
//
//
//        String nextState = transitionFunc.get(chosenLabel); // q1
//        log.info("nextState: " +nextState); // q1
//
//        Node chosenNode = null;
//
//        for (Relationship rel : currentNode.getRelationships()) {
//            log.info("Get Relationships: " + rel);
//            Node candidateNode = rel.getOtherNode(currentNode);
//            log.info(candidateNode.toString());
//            log.info(candidateNode.getLabels().toString());
//            String candidateLabel = candidateNode.getLabels().iterator().next().name();
//            log.info(candidateLabel);
////            if (candidateLabel.equals(nextState)) {
////                log.info("Entered if:");
//            chosenNode = candidateNode;
//            //  break;
//            //}
//        }
//        return chosenNode;
//    }
//
//}