package myophoria.api;

import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import myophoria.core.error.InvalidWorkoutInputException;
import myophoria.core.model.EquipmentType;
import myophoria.core.model.Exercise;
import myophoria.core.model.Laterality;
import myophoria.core.service.ExerciseService;

/** HTTP adapter for exercises (usage, permanent delete and progression arrive later). */
public class ExerciseController {
    /** Request body for create and update. */
    public record ExerciseRequest(String name, EquipmentType equipmentType, Laterality laterality) {
    }

    /** Exercise as sent to the browser; initials and weightLabel are computed in Java. */
    public record ExerciseDto(String id, String name, EquipmentType equipmentType, Laterality laterality,
            String initials, String weightLabel, boolean deleted) {
        static ExerciseDto of(Exercise e) {
            return new ExerciseDto(e.getId(), e.getName(), e.type(), e.getLaterality(), e.initials(),
                    e.weightLabel(), e.isDeleted());
        }
    }

    private final ExerciseService service;

    /** Creates the controller on the service. */
    public ExerciseController(ExerciseService service) {
        this.service = service;
    }

    /** Registers the routes. */
    public void register(RoutesConfig routes) {
        routes.get("/api/v1/exercises", this::list);
        routes.get("/api/v1/exercises/{id}", ctx -> ctx.json(ExerciseDto.of(service.get(ctx.pathParam("id")))));
        routes.post("/api/v1/exercises", ctx -> {
            ExerciseRequest r = ctx.bodyAsClass(ExerciseRequest.class);
            ctx.status(201).json(ExerciseDto.of(service.create(r.name(), r.equipmentType(), r.laterality())));
        });
        routes.put("/api/v1/exercises/{id}", ctx -> {
            ExerciseRequest r = ctx.bodyAsClass(ExerciseRequest.class);
            ctx.json(ExerciseDto.of(service.update(ctx.pathParam("id"), r.name(), r.equipmentType(), r.laterality())));
        });
        routes.delete("/api/v1/exercises/{id}", ctx -> {
            service.softDelete(ctx.pathParam("id"));
            ctx.status(204);
        });
        routes.post("/api/v1/exercises/{id}/restore",
                ctx -> ctx.json(ExerciseDto.of(service.restore(ctx.pathParam("id")))));
    }

    private void list(Context ctx) {
        List<ExerciseDto> result = new java.util.ArrayList<>();
        for (Exercise e : service.search(ctx.queryParam("q"), parseTypes(ctx.queryParam("type")),
                parseBoolean(ctx.queryParam("deleted")))) {
            result.add(ExerciseDto.of(e));
        }
        ctx.json(result);
    }

    private static Set<EquipmentType> parseTypes(String csv) {
        Set<EquipmentType> types = EnumSet.noneOf(EquipmentType.class);
        if (csv != null && !csv.isBlank()) {
            for (String part : csv.split(",")) {
                try {
                    types.add(EquipmentType.valueOf(part.trim()));
                } catch (IllegalArgumentException e) {
                    throw new InvalidWorkoutInputException("Unknown equipment type: " + part.trim());
                }
            }
        }
        return types;
    }

    private static boolean parseBoolean(String value) {
        if (value == null || value.equals("false")) {
            return false;
        }
        if (value.equals("true")) {
            return true;
        }
        throw new InvalidWorkoutInputException("deleted must be true or false");
    }
}
