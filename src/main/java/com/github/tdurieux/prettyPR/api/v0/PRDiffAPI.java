package com.github.tdurieux.prettyPR.api.v0;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.actions.model.Update;
import com.github.tdurieux.prettyPR.diff.PrettyPR;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import fr.inria.sacha.spoon.diffSpoon.CtDiff;
import fr.inria.sacha.spoon.diffSpoon.DiffSpoon;
import fr.inria.sacha.spoon.diffSpoon.DiffSpoonImpl;
import fr.inria.sacha.spoon.diffSpoon.SpoonGumTreeBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.reflect.Method;
import java.util.*;

import static com.github.tdurieux.prettyPR.diff.StringUtil.listStr2Str;

@Path("/")
public class PRDiffAPI {

    @GET
    @Path("{user}/{project}/{pr}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response analysePR(@PathParam("user") String user, @PathParam("project") String project, @PathParam("pr") int pr) {
        try {
            final PrettyPR prettyPR = new PrettyPR(user, project, pr);
            prettyPR.run();

            JSONObject output = new JSONObject();
            output.accumulate("user", user);
            output.accumulate("repository", project);
            JSONObject pullrequest = new JSONObject();
            pullrequest.accumulate("id", pr);
            pullrequest.accumulate("title", prettyPR.getPullRequest().getTitle());
            pullrequest.accumulate("body", prettyPR.getPullRequest().getBody());
            pullrequest.accumulate("url", prettyPR.getPullRequest().getUrl().getPath());
            output.accumulate("pullrequest", pullrequest);

            final Set<String> fileNames = prettyPR.getFiles();
            for (Iterator<String> iterator = fileNames.iterator(); iterator.hasNext(); ) {
                String filename = iterator.next();
                JSONObject change = new JSONObject();

                change.put("newFile", prettyPR.getNewFileContent().get(filename));
                change.put("oldFile", prettyPR.getOldFileContent().get(filename));

                change.accumulate("patch", prettyPR.getFileStringPatch().get(filename));
                JSONObject location = new JSONObject();
                location.accumulate("path", filename);
                change.accumulate("location", location);

                final CtType newCl;
                final CtType oldCl ;
                DiffSpoon diffSpoon = null;
                if (prettyPR.getNewTypes().get(filename) != null) {
                    newCl = prettyPR.getNewTypes().get(filename);
                    location.put("class", newCl.getQualifiedName());
                    location.put("type", getType(newCl));
                    diffSpoon = new DiffSpoonImpl(newCl.getFactory());
                } else {
                    newCl = null;
                }
                if (prettyPR.getOldTypes().get(filename) != null) {
                    oldCl = prettyPR.getOldTypes().get(filename);
                    if(diffSpoon == null) {
                        location.put("class", oldCl.getQualifiedName());
                        location.put("type", getType(oldCl));

                        diffSpoon = new DiffSpoonImpl(oldCl.getFactory());
                    }
                } else {
                    oldCl = null;
                    if(newCl == null){
                        location.accumulate("type", "Other");
                        diffSpoon = new DiffSpoonImpl();
                    }
                }
                CtDiff results = diffSpoon.compare(oldCl, newCl);
                CtElement ctElement = results.commonAncestor();
                List<Action> actions = results.getRootActions();
                if(actions.size() == 0) {
                    continue;
                }

                pullrequest.append("changes", change);
                for (Action action : actions) {
                    CtElement element = (CtElement) action.getNode().getMetadata(
                            SpoonGumTreeBuilder.SPOON_OBJECT);
                    JSONObject jsonAction = new JSONObject();
                    // action name
                    jsonAction.accumulate("action", action.getClass().getSimpleName());

                    // node type
                    String nodeType = element.getClass().getSimpleName();
                    nodeType = nodeType.substring(2, nodeType.length() - 4);
                    jsonAction.accumulate("nodeType", nodeType);

                    JSONObject actionPositionJSON = new JSONObject();
                    if(element.getPosition() != null) {
                        actionPositionJSON
                                .put("line", element.getPosition().getLine());
                        actionPositionJSON.put("sourceStart",
                                element.getPosition().getSourceStart());
                        actionPositionJSON.put("sourceEnd",
                                element.getPosition().getSourceEnd());
                        actionPositionJSON.put("endLine",
                                element.getPosition().getEndLine());

                    }
                    if (action instanceof Delete ||
                            action instanceof Update ||
                            action instanceof Move) {
                        jsonAction.put("oldLocation", actionPositionJSON);
                    } else {
                        jsonAction.put("newLocation", actionPositionJSON);
                    }

                    // action position
                    if (action instanceof Move ||
                            action instanceof Update) {
                        CtElement elementDest = (CtElement) action.getNode().getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT_DEST);

                        JSONObject actionDestPositionJSON = new JSONObject();
                        if(elementDest.getPosition() != null) {
                            actionDestPositionJSON.put("line",
                                    elementDest.getPosition().getLine());
                            actionDestPositionJSON.put("sourceStart",
                                    elementDest.getPosition().getSourceStart());
                            actionDestPositionJSON.put("sourceEnd",
                                    elementDest.getPosition().getSourceEnd());
                            actionDestPositionJSON.put("endLine",
                                    elementDest.getPosition().getEndLine());
                        }
                        jsonAction.put("newLocation", actionDestPositionJSON);
                    }

                    // if all actions are applied on the same node print only the first action
                    if (element.equals(ctElement) && action instanceof Update) {
                        break;
                    }
                    change.append("actions", jsonAction);
                    System.out.println(jsonAction.toString(2));
                }

            }
            return Response.status(201).entity(output.toString(2)).build();
        } catch (Exception e) {
            e.printStackTrace();
            JSONObject error = new JSONObject();
            error.accumulate("message", e.getMessage());
            error.accumulate("exception", e);
            return Response.status(500).entity(error.toString()).build();
        }
    }

    private String getType(CtType ctType) {
        String simpleName = ctType.getClass().getSimpleName();
        String type = simpleName.substring(2, simpleName.length()- 4);
        if(ctType.getSimpleName().contains("Test")){
            type = "Test";
        }
        return type;
    }
}
