import React, { useState } from 'react';
import {
  useGetEntryStats,
  useListEntries,
  useDeleteEntry,
  getListEntriesQueryKey,
  getGetEntryStatsQueryKey,
  EntryCategory,
} from '@workspace/api-client-react';
import { useQueryClient } from '@tanstack/react-query';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Checkbox } from '@/components/ui/checkbox';
import { Separator } from '@/components/ui/separator';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { Trash2, Loader2, MapPin, Leaf } from 'lucide-react';
import { formatDistanceToNow, format } from 'date-fns';
import { CATEGORY_COLORS, CATEGORY_ICONS, CATEGORY_LABELS } from './MapComponent';
import { toast } from 'sonner';

interface LeftSidebarProps {
  visibleCategories: Set<string>;
  setVisibleCategories: (categories: Set<string>) => void;
  selectedEntryId: number | null;
  onSelectEntry: (id: number, lat: number, lng: number) => void;
}

export function LeftSidebar({
  visibleCategories,
  setVisibleCategories,
  selectedEntryId,
  onSelectEntry,
}: LeftSidebarProps) {
  const { data: stats, isLoading: statsLoading } = useGetEntryStats();
  const { data: entries = [], isLoading: entriesLoading } = useListEntries();
  const deleteEntryMutation = useDeleteEntry();
  const queryClient = useQueryClient();

  const [confirmDeleteId, setConfirmDeleteId] = useState<number | null>(null);

  const toggleCategory = (category: string) => {
    const next = new Set(visibleCategories);
    if (next.has(category)) {
      next.delete(category);
    } else {
      next.add(category);
    }
    setVisibleCategories(next);
  };

  const toggleAll = () => {
    if (visibleCategories.size === Object.keys(CATEGORY_LABELS).length) {
      setVisibleCategories(new Set());
    } else {
      setVisibleCategories(new Set(Object.keys(CATEGORY_LABELS)));
    }
  };

  const confirmDelete = () => {
    if (confirmDeleteId === null) return;
    const entry = entries.find((e) => e.id === confirmDeleteId);
    deleteEntryMutation.mutate(
      { id: confirmDeleteId },
      {
        onSuccess: () => {
          queryClient.invalidateQueries({ queryKey: getListEntriesQueryKey() });
          queryClient.invalidateQueries({ queryKey: getGetEntryStatsQueryKey() });
          toast.success(`${CATEGORY_LABELS[entry?.category ?? '']} removed`);
          setConfirmDeleteId(null);
        },
        onError: () => {
          toast.error('Failed to delete entry.');
          setConfirmDeleteId(null);
        },
      }
    );
  };

  // Count per category for filter labels
  const categoryCounts = React.useMemo(() => {
    const counts: Record<string, number> = {};
    for (const e of entries) {
      counts[e.category] = (counts[e.category] ?? 0) + 1;
    }
    return counts;
  }, [entries]);

  const filteredEntries = entries
    .filter((e) => visibleCategories.has(e.category))
    .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());

  const allChecked = visibleCategories.size === Object.keys(CATEGORY_LABELS).length;

  return (
    <>
      <div className="w-80 h-full flex flex-col bg-card shadow-2xl relative z-20 border-r border-border shrink-0">
        {/* Header */}
        <div className="px-5 pt-5 pb-4">
          <div className="flex items-center gap-2 mb-0.5">
            <div className="w-7 h-7 rounded-lg bg-primary flex items-center justify-center">
              <Leaf className="w-4 h-4 text-primary-foreground" />
            </div>
            <h1 className="text-xl font-serif font-bold text-foreground">Field Journal</h1>
          </div>
          <p className="text-xs text-muted-foreground ml-9">Community Environmental Tracking</p>
        </div>

        {/* Stats grid */}
        <div className="px-5 pb-4">
          <p className="text-[10px] font-semibold uppercase tracking-widest text-muted-foreground mb-2.5">
            Overview
          </p>
          {statsLoading ? (
            <div className="flex items-center justify-center py-5">
              <Loader2 className="w-5 h-5 animate-spin text-muted-foreground" />
            </div>
          ) : (
            <div className="grid grid-cols-2 gap-2">
              {[
                { key: EntryCategory.tree_planted, label: 'Trees Planted' },
                { key: EntryCategory.mangrove_dying, label: 'Dying Mangroves' },
                { key: EntryCategory.trash_pile, label: 'Trash Piles' },
              ].map(({ key, label }) => (
                <div
                  key={key}
                  className="rounded-xl p-3 border border-border bg-background flex flex-col gap-1 cursor-pointer hover:border-current transition-colors"
                  style={{ '--tw-border-opacity': 1 } as React.CSSProperties}
                  onClick={() => toggleCategory(key)}
                >
                  <div className="flex items-center justify-between">
                    <div
                      className="w-6 h-6 rounded-full flex items-center justify-center text-white"
                      style={{ backgroundColor: CATEGORY_COLORS[key] }}
                    >
                      <span style={{ transform: 'scale(0.75)', display: 'flex' }}>
                        {CATEGORY_ICONS[key]}
                      </span>
                    </div>
                    <span
                      className="text-2xl font-bold tabular-nums"
                      style={{ color: CATEGORY_COLORS[key] }}
                    >
                      {(stats as any)?.[key] ?? 0}
                    </span>
                  </div>
                  <p className="text-[11px] text-muted-foreground leading-tight">{label}</p>
                </div>
              ))}
              <div className="rounded-xl p-3 border border-border bg-background flex flex-col gap-1">
                <div className="flex items-center justify-between">
                  <MapPin className="w-5 h-5 text-muted-foreground" />
                  <span className="text-2xl font-bold tabular-nums text-foreground">
                    {stats?.total ?? 0}
                  </span>
                </div>
                <p className="text-[11px] text-muted-foreground leading-tight">Total Reports</p>
              </div>
            </div>
          )}
        </div>

        <Separator />

        {/* Filters */}
        <div className="px-5 py-3">
          <div className="flex items-center justify-between mb-2">
            <p className="text-[10px] font-semibold uppercase tracking-widest text-muted-foreground">
              Filters
            </p>
            <button
              onClick={toggleAll}
              className="text-[10px] text-primary hover:underline"
            >
              {allChecked ? 'Hide all' : 'Show all'}
            </button>
          </div>
          <div className="space-y-2">
            {Object.entries(CATEGORY_LABELS).map(([cat, label]) => (
              <div key={cat} className="flex items-center gap-2.5">
                <Checkbox
                  id={`filter-${cat}`}
                  checked={visibleCategories.has(cat)}
                  onCheckedChange={() => toggleCategory(cat)}
                />
                <label
                  htmlFor={`filter-${cat}`}
                  className="flex-1 text-sm leading-none flex items-center gap-2 cursor-pointer"
                >
                  <div
                    className="w-2.5 h-2.5 rounded-full flex-shrink-0"
                    style={{ backgroundColor: CATEGORY_COLORS[cat] }}
                  />
                  {label}
                </label>
                {categoryCounts[cat] ? (
                  <span className="text-[11px] tabular-nums font-medium text-muted-foreground bg-muted rounded-full px-1.5 py-0.5 min-w-[22px] text-center">
                    {categoryCounts[cat]}
                  </span>
                ) : null}
              </div>
            ))}
          </div>
        </div>

        <Separator />

        {/* Entries list */}
        <div className="flex-1 overflow-hidden flex flex-col min-h-0">
          <div className="px-5 py-3 flex items-center justify-between">
            <p className="text-[10px] font-semibold uppercase tracking-widest text-muted-foreground">
              Recent Entries
            </p>
            {filteredEntries.length > 0 && (
              <span className="text-[11px] text-muted-foreground">
                {filteredEntries.length} shown
              </span>
            )}
          </div>

          <ScrollArea className="flex-1">
            {entriesLoading ? (
              <div className="flex items-center justify-center p-8">
                <Loader2 className="w-5 h-5 animate-spin text-muted-foreground" />
              </div>
            ) : filteredEntries.length === 0 ? (
              <div className="p-6 text-center text-muted-foreground">
                <MapPin className="w-8 h-8 mx-auto mb-2 opacity-20" />
                <p className="text-sm font-medium">No observations yet</p>
                <p className="text-xs mt-1 opacity-70">
                  {entries.length === 0
                    ? 'Tap anywhere on the map to log your first entry.'
                    : 'Enable a filter above to see entries.'}
                </p>
              </div>
            ) : (
              <div className="divide-y divide-border">
                {filteredEntries.map((entry) => {
                  const isSelected = entry.id === selectedEntryId;
                  return (
                    <div
                      key={entry.id}
                      className={`px-5 py-3.5 cursor-pointer transition-colors group flex items-start gap-3 ${
                        isSelected
                          ? 'bg-primary/8 border-l-2 border-primary'
                          : 'hover:bg-muted/40 border-l-2 border-transparent'
                      }`}
                      onClick={() => onSelectEntry(entry.id, entry.lat, entry.lng)}
                    >
                      <div
                        className="w-8 h-8 rounded-full flex-shrink-0 flex items-center justify-center text-white mt-0.5 shadow-sm"
                        style={{ backgroundColor: CATEGORY_COLORS[entry.category] }}
                      >
                        {CATEGORY_ICONS[entry.category]}
                      </div>

                      <div className="flex-1 min-w-0">
                        <div className="flex justify-between items-start gap-1">
                          <p className="font-medium text-sm text-foreground leading-tight">
                            {CATEGORY_LABELS[entry.category]}
                          </p>
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              setConfirmDeleteId(entry.id);
                            }}
                            className="text-muted-foreground hover:text-destructive opacity-0 group-hover:opacity-100 transition-opacity flex-shrink-0 mt-0.5"
                            title="Delete entry"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        </div>

                        <p className="text-[11px] text-muted-foreground mt-0.5">
                          {formatDistanceToNow(new Date(entry.date), { addSuffix: true })} ·{' '}
                          {entry.reporter}
                        </p>

                        {entry.notes && (
                          <p className="text-xs text-muted-foreground line-clamp-2 mt-1 leading-relaxed italic">
                            "{entry.notes}"
                          </p>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </ScrollArea>
        </div>

        {/* Footer hint */}
        <div className="px-5 py-3 border-t border-border bg-muted/20">
          <p className="text-[10px] text-muted-foreground text-center">
            Tap anywhere on the map to log a new observation
          </p>
        </div>
      </div>

      {/* Confirm delete dialog */}
      <AlertDialog open={confirmDeleteId !== null} onOpenChange={(open) => !open && setConfirmDeleteId(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete this entry?</AlertDialogTitle>
            <AlertDialogDescription>
              This will permanently remove the observation from the map. This action cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={confirmDelete}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              {deleteEntryMutation.isPending ? (
                <Loader2 className="w-4 h-4 animate-spin mr-2" />
              ) : null}
              Delete
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
