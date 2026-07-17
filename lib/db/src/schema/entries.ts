import { pgTable, serial, doublePrecision, text, varchar, timestamp } from "drizzle-orm/pg-core";
import { createInsertSchema } from "drizzle-zod";
import { z } from "zod/v4";

export const entriesTable = pgTable("entries", {
  id: serial("id").primaryKey(),
  lat: doublePrecision("lat").notNull(),
  lng: doublePrecision("lng").notNull(),
  category: varchar("category", { length: 50 }).notNull(), // tree_planted | mangrove_dying | trash_pile
  notes: text("notes"),
  reporter: varchar("reporter", { length: 255 }).notNull(),
  date: timestamp("date").defaultNow().notNull(),
});

export const insertEntrySchema = createInsertSchema(entriesTable).omit({ id: true, date: true });
export type InsertEntry = z.infer<typeof insertEntrySchema>;
export type Entry = typeof entriesTable.$inferSelect;
