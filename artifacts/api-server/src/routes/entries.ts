import { Router } from "express";
import { db, entriesTable } from "@workspace/db";
import { eq, sql } from "drizzle-orm";
import {
  CreateEntryBody,
  DeleteEntryParams,
} from "@workspace/api-zod";

const router = Router();

// GET /api/entries — return all entries
router.get("/entries", async (req, res) => {
  try {
    const entries = await db
      .select()
      .from(entriesTable)
      .orderBy(entriesTable.date);

    const formatted = entries.map((e) => ({
      ...e,
      date: e.date.toISOString(),
    }));

    res.json(formatted);
  } catch (err) {
    req.log.error({ err }, "Failed to list entries");
    res.status(500).json({ error: "Failed to fetch entries" });
  }
});

// GET /api/entries/stats — counts per category
router.get("/entries/stats", async (req, res) => {
  try {
    const rows = await db
      .select({
        category: entriesTable.category,
        count: sql<number>`cast(count(*) as int)`,
      })
      .from(entriesTable)
      .groupBy(entriesTable.category);

    const stats = {
      tree_planted: 0,
      mangrove_dying: 0,
      trash_pile: 0,
      total: 0,
    };

    for (const row of rows) {
      const cat = row.category as keyof typeof stats;
      if (cat in stats) {
        stats[cat] = row.count;
        stats.total += row.count;
      }
    }

    res.json(stats);
  } catch (err) {
    req.log.error({ err }, "Failed to get entry stats");
    res.status(500).json({ error: "Failed to fetch stats" });
  }
});

// POST /api/entries — create a new entry
router.post("/entries", async (req, res) => {
  const parsed = CreateEntryBody.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: "Invalid entry data" });
    return;
  }

  const { lat, lng, category, notes, reporter } = parsed.data;

  try {
    const [entry] = await db
      .insert(entriesTable)
      .values({ lat, lng, category, notes: notes ?? null, reporter })
      .returning();

    res.status(201).json({ ...entry, date: entry.date.toISOString() });
  } catch (err) {
    req.log.error({ err }, "Failed to create entry");
    res.status(500).json({ error: "Failed to create entry" });
  }
});

// DELETE /api/entries/:id — remove an entry
router.delete("/entries/:id", async (req, res) => {
  const parsed = DeleteEntryParams.safeParse({ id: Number(req.params.id) });
  if (!parsed.success) {
    res.status(400).json({ error: "Invalid entry ID" });
    return;
  }

  try {
    const [deleted] = await db
      .delete(entriesTable)
      .where(eq(entriesTable.id, parsed.data.id))
      .returning();

    if (!deleted) {
      res.status(404).json({ error: "Entry not found" });
      return;
    }

    res.json({ ...deleted, date: deleted.date.toISOString() });
  } catch (err) {
    req.log.error({ err }, "Failed to delete entry");
    res.status(500).json({ error: "Failed to delete entry" });
  }
});

export default router;
