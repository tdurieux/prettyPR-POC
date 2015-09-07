package com.github.tdurieux.prettyPR.api.v0;

import com.github.tdurieux.prettyPR.diff.PrettyPR;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import org.json.JSONArray;
import org.json.JSONObject;
import spoon.reflect.declaration.CtMethod;
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


            List<CtMethod> changedMethod = new ArrayList<CtMethod>();
            Set<CtType> changedCtType = new HashSet<CtType>();

            final Set<String> fileNames = prettyPR.getFilePatch().keySet();
            for (Iterator<String> iterator = fileNames.iterator(); iterator.hasNext(); ) {
                String filename = iterator.next();
                JSONObject change = new JSONObject();
                pullrequest.append("changes", change);
                change.accumulate("patch", prettyPR.getFileStringPatch().get(filename));
                JSONObject location = new JSONObject();
                location.accumulate("path", filename);
                change.accumulate("location", location);
                if(prettyPR.getNewTypes().get(filename)!= null) {
                    final CtType cl = prettyPR.getNewTypes().get(filename);
                    changedCtType.add(cl);
                    location.accumulate("class", cl.getQualifiedName());
                    location.accumulate("type", getType(cl));
                } else if(prettyPR.getOldTypes().get(filename)!=null) {
                    final CtType cl = prettyPR.getOldTypes().get(filename);
                    changedCtType.add(cl);
                    location.accumulate("class", cl.getQualifiedName());
                    location.accumulate("type", getType(cl));
                } else {
                    location.accumulate("type", "Other");
                }
                JSONArray changedMethods = new JSONArray();
                change.accumulate("changedMethods", changedMethods);

                final Patch<String> stringPatch = prettyPR.getFilePatch().get(filename);
                final List<Delta<String>> deltas = stringPatch.getDeltas();
                for (int i = 0; i < deltas.size(); i++) {
                    Delta<String> stringDelta = deltas.get(i);

                    int startLine = stringDelta.getRevised().getPosition() + 1;
                    int endLine = stringDelta.getRevised().last() + 1;
                    final List<String> lines = stringDelta.getRevised().getLines();
                    for (int j = 0; j < lines.size(); j++) {
                        if(j >= stringDelta.getOriginal().getLines().size()) {
                            break;
                        }
                        String s = lines.get(j);
                        if(stringDelta.getOriginal().getLines().get(j).equals(s)) {
                            startLine ++;
                        } else {
                            break;
                        }
                    }
                    int diff = lines.size() - stringDelta.getOriginal().getLines().size();
                    if(stringDelta.getOriginal().getLines().size() > 0) {
                        for (int j = lines.size() - 1; j >= 0; j--) {
                            String s = lines.get(j);
                            if (j - diff >= 0 && stringDelta.getOriginal().getLines().get(j - diff).equals(s)) {
                                endLine--;
                            } else {
                                break;
                            }
                        }
                    }

                    CtType ctType = prettyPR.getNewTypes().get(filename);
                    if(ctType == null) {
                        ctType = prettyPR.getOldTypes().get(filename);
                    }
                    if(ctType == null) {
                        continue;
                    }

                    final Set<CtMethod> methods = ctType.getMethods();

                    boolean find = false;
                    for (Iterator iterator1 = methods.iterator(); iterator1.hasNext(); ) {
                        final CtMethod ctMethod = (CtMethod) iterator1.next();
                        int startM = ctMethod.getPosition().getLine();
                        int endM = ctMethod.getPosition().getEndLine();
                        if(ctMethod.getBody() != null) {
                            endM = ctMethod.getBody().getPosition().getEndLine();
                        }
                        if((startLine < startM && startM < endLine && endLine < endM) ||
                                   (startLine >= startM && endLine <= endM) ||
                                   (startLine < endM && endM < endLine && startM < startLine ) ||
                                   (startLine <= startM && endLine >= endM)){
                            find = true;
                            JSONObject jsonChangedMethod = new JSONObject();
                            changedMethods.put(jsonChangedMethod);
                            jsonChangedMethod.accumulate("method", ctMethod.getSimpleName());
                            final Method getDeltaText = DiffUtils.class.getDeclaredMethod("getDeltaText", Delta.class);
                            if(getDeltaText!=null) {
                                getDeltaText.setAccessible(true);
                                jsonChangedMethod.accumulate("patch", listStr2Str((List<String>) getDeltaText.invoke(null, stringDelta)));
                            }
                            changedMethod.add(ctMethod);
                            System.out.println(ctMethod.getSimpleName() + " " + stringDelta.getType());
                        }
                    }
                    if(find == false) {
                        System.out.println("No method found for the change " + stringDelta);
                    }
                }
            }
            return Response.status(201).entity(output.toString()).build();
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
