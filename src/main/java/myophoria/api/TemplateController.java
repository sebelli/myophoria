package myophoria.api;

import io.javalin.config.RoutesConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import myophoria.api.ExerciseController.ExerciseDto;
import myophoria.core.model.Exercise;
import myophoria.core.model.WorkoutTemplate;
import myophoria.core.service.ExerciseService;
import myophoria.core.service.TemplateService;

/** HTTP adapter for templates, including "save as template" from a session. */
public class TemplateController {
    /** Request body for create and update. */
    public record TemplateRequest(String name, List<String> exerciseIds) {
    }

    /** Request body for saving a session as a template. */
    public record SaveRequest(String name) {
    }

    /** Template with its (visible) exercises in order. */
    public record TemplateDto(String id, String name, List<ExerciseDto> exercises) {
        static TemplateDto of(WorkoutTemplate t, Map<String, Exercise> exercises) {
            List<ExerciseDto> list = new ArrayList<>();
            for (String exerciseId : t.getExerciseIds()) {
                list.add(ExerciseDto.of(exercises.get(exerciseId)));
            }
            return new TemplateDto(t.getId(), t.getName(), list);
        }
    }

    private final TemplateService templates;
    private final ExerciseService exercises;

    /** Creates the controller on its services. */
    public TemplateController(TemplateService templates, ExerciseService exercises) {
        this.templates = templates;
        this.exercises = exercises;
    }

    /** Registers the routes. */
    public void register(RoutesConfig routes) {
        routes.get("/api/v1/templates", ctx -> {
            Map<String, Exercise> byId = exercises.allById();
            List<TemplateDto> result = new ArrayList<>();
            for (WorkoutTemplate t : templates.list()) {
                result.add(TemplateDto.of(t, byId));
            }
            ctx.json(result);
        });
        routes.post("/api/v1/templates", ctx -> {
            TemplateRequest r = ctx.bodyAsClass(TemplateRequest.class);
            ctx.status(201).json(TemplateDto.of(templates.create(r.name(), r.exerciseIds()), exercises.allById()));
        });
        routes.put("/api/v1/templates/{id}", ctx -> {
            TemplateRequest r = ctx.bodyAsClass(TemplateRequest.class);
            ctx.json(TemplateDto.of(templates.update(ctx.pathParam("id"), r.name(), r.exerciseIds()),
                    exercises.allById()));
        });
        routes.delete("/api/v1/templates/{id}", ctx -> {
            templates.delete(ctx.pathParam("id"));
            ctx.status(204);
        });
        routes.post("/api/v1/sessions/{id}/template", ctx -> {
            SaveRequest r = ctx.bodyAsClass(SaveRequest.class);
            ctx.status(201).json(TemplateDto.of(templates.saveFromSession(ctx.pathParam("id"), r.name()),
                    exercises.allById()));
        });
    }
}
