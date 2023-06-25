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
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//import java.util.stream.Stream;
//
//public class GetRegPatternWalks {
//
//    @Context
//    public Log log;
//
//    @Context
//    public Transaction transaction;
//
//    @Procedure(value = "regpattern2vec.GetRegPatternWalks")
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
//        // Compile the pattern into a regular expression
//        Pattern regex = Pattern.compile(pattern);
//
//        log.info("Regex: " + regex);
//
//
//
//        // Extract the DFA as a string
//        String dfa = regex.pattern().substring(2, regex.pattern().length() - 2);
//
//        log.info("dfa generated: "+ dfa);
//
//
//        //String dfaUse = dfaUsed.substring(1, dfaUsed.length() - 1);
//
//
//        //{q1={Author=q2, Paper=q1}, q2={Author=q0, Paper=q1)}, (q0={Author=q0, Paper=q1}}
//        //{q0={Author=q0, Paper=q1}, q2={Author=q0, Paper=q1}, q1={Author=q2, Paper=q1}}
//
//        String dfaUsed = "(q0,Author->q0|Paper->q1),(q1,Author->q2|Paper->q1),(q2,Author->q1|Paper->q1)";
//        //String[] transitions = Arrays.copyOfRange(dfaParts, 1, dfaParts.length); // extract transitions
//
//        String extractLabel = dfaUsed.substring(dfaUsed.indexOf(',') + 1, dfaUsed.indexOf("->"));
//        log.info(extractLabel);
//        String startNodeLabel = extractLabel;
//        log.info("Start Node" + startNodeLabel);
//        log.info("In transaction first label:" + String.valueOf(Label.label(startNodeLabel)));
//
//        Map<String, Map<String, String>> transitionFunc = computeTransitionFunction(dfaUsed);
//        log.info("Transition function: " + transitionFunc);
//
//        Walks wks = new Walks();
//        wks.walks = new ArrayList<>();
//
//        // for each starting node of the specified label
//        //String dfa = "(q0,Author->q0|Paper->q1),(q1,Author->q2|Paper->q1),(q2,Author->q1|Paper->q1)";
//        transaction.findNodes(Label.label(startNodeLabel))
//                .forEachRemaining(startNode -> {
//                    for (int i = 0; i < walksNumber; i++) {
//                        wks.walks.add(
//                                getRegPatternWalk(startNode, walkLengthInt, transitionFunc)
//                        );
//                    }
//                });
//
//        return Stream.of(wks);
//    }
//    private Map<String, Map<String, String>> computeTransitionFunction(String dfa) {
//        Map<String, Map<String, String>> transitionFunc = new HashMap<>();
//        Set<String> inputSymbols = new HashSet<>();
//
//        // Split the DFA into states and transitions
//        String[] stateTransitions = dfa.split("\\),\\(");
//
//        // Iterate over the transitions to populate the transition function and input symbols set
//        for (String stateTransition : stateTransitions) {
//            // Split the state and its transitions
//            String[] parts = stateTransition.split(",");
//            String state = parts[0];
//            String transitions = parts[1];
//
//            // Create a new map to store the transitions for the state
//            Map<String, String> stateTransitionsMap = new HashMap<>();
//
//            // Split the transitions into individual transitions
//            String[] transitionList = transitions.split("\\|");
//
//            // Iterate over the transitions to populate the transition map for the state
//            for (String transition : transitionList) {
//                // Split the transition into label and next state
//                String[] transitionParts = transition.split("->");
//                String label = transitionParts[0];
//                String nextState = transitionParts[1];
//
//                // Add the transition to the transition map
//                stateTransitionsMap.put(label, nextState);
//
//                // Add the input symbol to the input symbols set
//                inputSymbols.add(label);
//            }
//
//            // Add the transition map for the state to the overall transition function
//            transitionFunc.put(state, stateTransitionsMap);
//        }
//
//        // Create node labels from the input symbols set
//        for (String symbol : inputSymbols) {
//            Label label = Label.label(symbol);
//            // You can use this label to create nodes in your graph with the same label name
//        }
//
//        return transitionFunc;
//    }
//
//    private String getRegPatternWalk(Node currentNode, Long remainingSteps, Map<String, Map<String, String>> transitionFunc) {
//        String propertyKey = "";
//        if (remainingSteps == 0) {
//            propertyKey = currentNode.getLabels().iterator().next().name() + "_" + currentNode.getId();
//            return propertyKey;
//        }
//
//        String currentLabel = currentNode.getLabels().iterator().next().name();
//        log.info("Current Label: " + currentLabel);
//        Map<String, String> currentTransitions = transitionFunc.get(currentLabel);
//        if (currentTransitions == null || currentTransitions.isEmpty()) {
//            // Reached a dead end, stop the walk
//            propertyKey = currentNode.getLabels().iterator().next().name() + "_" + currentNode.getId();
//            return propertyKey;
//        }
//
//        // Choose the next label based on the transition probabilities
//        String chosenLabel = chooseLabel(currentTransitions);
//
//        // Choose the next node with the chosen label
//        Node chosenNode = chooseNode(currentNode, chosenLabel, transitionFunc);
//
//        // Recursively continue the walk
//        String propertyVal = chosenNode.getLabels().iterator().next().name() + "_" + chosenNode.getId();
//        return propertyVal + " " + getRegPatternWalk(chosenNode, remainingSteps - 1, transitionFunc);
//    }
//
//    private String chooseLabel(Map<String, String> currentTransitions) {
//        // Compute the total weight of all transitions
//        double totalWeight = currentTransitions.values().stream().mapToDouble(Double::parseDouble).sum();
//
//        // Generate a random number between 0 and the total weight
//        double randomValue = Math.random() * totalWeight;
//
//        // Choose the label based on the random value and the transition probabilities
//        double cumulativeWeight = 0;
//        for (Map.Entry<String, String> transition : currentTransitions.entrySet()) {
//            cumulativeWeight += Double.parseDouble(transition.getValue());
//            if (randomValue <= cumulativeWeight) {
//                return transition.getKey();
//            }
//        }
//
//        // Should never reach this point, but return the first label as a fallback
//        return currentTransitions.keySet().iterator().next();
//    }
//    private Node chooseNode(Node currentNode, String chosenLabel, Map<String, Map<String, String>> transitionFunc) {
//        String currentLabel = currentNode.getLabels().iterator().next().name();
//        Map<String, String> currentTransitions = transitionFunc.get(currentLabel);
//        String nextState = currentTransitions.get(chosenLabel);
//        Node chosenNode = null;
//        for (Relationship rel : currentNode.getRelationships()) {
//            Node candidateNode = rel.getOtherNode(currentNode);
//            String candidateLabel = candidateNode.getLabels().iterator().next().name();
//            if (candidateLabel.equals(nextState)) {
//                chosenNode = candidateNode;
//                break;
//            }
//        }
//        return chosenNode;
//    }
//
//
//}