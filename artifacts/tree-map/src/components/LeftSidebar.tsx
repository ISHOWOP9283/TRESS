import React from 'react';
import { useGetEntryStats, useListEntries, useDeleteEntry, getListEntriesQueryKey, getGetEntryStatsQueryKey, EntryCategory } from '@workspace/api-client-react';
import { useQueryClient } from '@tanstack/react-query';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Checkbox } from '@/components/ui/checkbox';
import { Separator } from '@/components/ui/separator';
import { Trash2, Loader2, MapPin } from 'lucide-react';
import { format } from 'date-fns';
import { CATEGORY_COLORS, CATEGORY_ICONS, CATEGORY_LABELS } from './MapComponent';

interface LeftSidebarProps {
  visibleCategories: Set<string>;
  setVisibleCategories: (categories: Set<string>) => void;
  onSelectEntry: (lat: number, lng: number) => void;
}

export function LeftSidebar({ visibleCategories, setVisibleCategories, onSelectEntry }: LeftSidebarProps) {
  const { data: stats, isLoading: statsLoading } = useGetEntryStats();
  const { data: entries = [], isLoading: entriesLoading } = useListEntries();
  const deleteEntryMutation = useDeleteEntry();
  const queryClient = useQueryClient();

  const toggleCategory = (category: string) => {
    const newCategories = new Set(visibleCategories);
    if (newCategories.has(category)) {
      newCategories.delete(category);
    } else {
      newCategories.add(category);
    }
    setVisibleCategories(newCategories);
  };

  const handleDelete = (id: number, e: React.MouseEvent) => {
    e.stopPropagation();
    deleteEntryMutation.mutate({ id }, {
      onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: getListEntriesQueryKey() });
        queryClient.invalidateQueries({ queryKey: getGetEntryStatsQueryKey() });
      }
    });
  };

  const filteredEntries = entries.filter(e => visibleCategories.has(e.category))
    .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());

  return (
    <div className="w-96 h-full flex flex-col bg-card shadow-2xl relative z-20 border-r border-border">
      <div className="p-6 pb-4">
        <h1 className="text-2xl font-serif font-bold text-foreground mb-1">Field Journal</h1>
        <p className="text-sm text-muted-foreground">Community Environmental Tracking</p>
      </div>

      <div className="px-6 py-4 bg-muted/30">
        <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground mb-3">Overview</h2>
        
        {statsLoading ? (
          <div className="flex items-center justify-center py-4">
            <Loader2 className="w-5 h-5 animate-spin text-muted-foreground" />
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-3 mb-4">
            <div className="bg-background rounded-lg p-3 border border-border">
              <div className="text-2xl font-semibold mb-1" style={{ color: CATEGORY_COLORS[EntryCategory.tree_planted] }}>{stats?.tree_planted || 0}</div>
              <div className="text-xs text-muted-foreground">Trees Planted</div>
            </div>
            <div className="bg-background rounded-lg p-3 border border-border">
              <div className="text-2xl font-semibold mb-1" style={{ color: CATEGORY_COLORS[EntryCategory.mangrove_dying] }}>{stats?.mangrove_dying || 0}</div>
              <div className="text-xs text-muted-foreground">Dying Mangroves</div>
            </div>
            <div className="bg-background rounded-lg p-3 border border-border">
              <div className="text-2xl font-semibold mb-1" style={{ color: CATEGORY_COLORS[EntryCategory.trash_pile] }}>{stats?.trash_pile || 0}</div>
              <div className="text-xs text-muted-foreground">Trash Piles</div>
            </div>
            <div className="bg-background rounded-lg p-3 border border-border">
              <div className="text-2xl font-semibold mb-1 text-foreground">{stats?.total || 0}</div>
              <div className="text-xs text-muted-foreground">Total Reports</div>
            </div>
          </div>
        )}

        <div className="space-y-3 mt-6">
          <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground mb-2">Filters</h2>
          {Object.entries(CATEGORY_LABELS).map(([cat, label]) => (
            <div key={cat} className="flex items-center space-x-2">
              <Checkbox 
                id={`filter-${cat}`} 
                checked={visibleCategories.has(cat)}
                onCheckedChange={() => toggleCategory(cat)}
              />
              <label 
                htmlFor={`filter-${cat}`}
                className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70 flex items-center gap-2 cursor-pointer"
              >
                <div 
                  className="w-3 h-3 rounded-full" 
                  style={{ backgroundColor: CATEGORY_COLORS[cat] }} 
                />
                {label}
              </label>
            </div>
          ))}
        </div>
      </div>

      <Separator />

      <div className="flex-1 overflow-hidden flex flex-col">
        <div className="p-4 bg-card">
          <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">Recent Entries</h2>
        </div>
        
        <ScrollArea className="flex-1">
          {entriesLoading ? (
            <div className="flex items-center justify-center p-8">
              <Loader2 className="w-6 h-6 animate-spin text-muted-foreground" />
            </div>
          ) : filteredEntries.length === 0 ? (
            <div className="p-8 text-center text-muted-foreground">
              <MapPin className="w-8 h-8 mx-auto mb-2 opacity-20" />
              <p className="text-sm">No entries found for selected filters.</p>
            </div>
          ) : (
            <div className="divide-y divide-border">
              {filteredEntries.map(entry => (
                <div 
                  key={entry.id} 
                  className="p-4 hover:bg-muted/30 cursor-pointer transition-colors group flex items-start gap-3"
                  onClick={() => onSelectEntry(entry.lat, entry.lng)}
                >
                  <div 
                    className="w-8 h-8 rounded-full flex-shrink-0 flex items-center justify-center text-white mt-1 shadow-sm"
                    style={{ backgroundColor: CATEGORY_COLORS[entry.category] }}
                  >
                    {CATEGORY_ICONS[entry.category]}
                  </div>
                  
                  <div className="flex-1 min-w-0">
                    <div className="flex justify-between items-start mb-1">
                      <p className="font-medium text-sm text-foreground truncate">
                        {CATEGORY_LABELS[entry.category]}
                      </p>
                      <button
                        onClick={(e) => handleDelete(entry.id, e)}
                        className="text-muted-foreground hover:text-destructive opacity-0 group-hover:opacity-100 transition-opacity"
                        title="Delete entry"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                    
                    <p className="text-xs text-muted-foreground mb-1">
                      {format(new Date(entry.date), 'MMM d, yyyy')} • by {entry.reporter}
                    </p>
                    
                    {entry.notes && (
                      <p className="text-sm text-muted-foreground line-clamp-2 mt-2 leading-relaxed">
                        "{entry.notes}"
                      </p>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </ScrollArea>
      </div>
    </div>
  );
}
