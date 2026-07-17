import React, { useState, useCallback, useMemo } from 'react';
import { useListEntries, useGetEntryStats, useDeleteEntry, getListEntriesQueryKey, getGetEntryStatsQueryKey } from '@workspace/api-client-react';
import { useQueryClient } from '@tanstack/react-query';
import { Map, AdvancedMarker, InfoWindow, useMapsLibrary } from '@vis.gl/react-google-maps';
import { EntryCategory, type Entry } from '@workspace/api-client-react';
import { TreePine, Droplets, Trash2, X, Plus } from 'lucide-react';
import { format } from 'date-fns';

export const CATEGORY_COLORS: Record<string, string> = {
  [EntryCategory.tree_planted]: '#2f5c35', // green
  [EntryCategory.mangrove_dying]: '#8b7355', // brown
  [EntryCategory.trash_pile]: '#b84a39', // red
};

export const CATEGORY_LABELS: Record<string, string> = {
  [EntryCategory.tree_planted]: 'Tree Planted',
  [EntryCategory.mangrove_dying]: 'Mangrove Dying',
  [EntryCategory.trash_pile]: 'Trash Pile',
};

export const CATEGORY_ICONS: Record<string, React.ReactNode> = {
  [EntryCategory.tree_planted]: <TreePine className="w-4 h-4" />,
  [EntryCategory.mangrove_dying]: <Droplets className="w-4 h-4" />,
  [EntryCategory.trash_pile]: <Trash2 className="w-4 h-4" />,
};

interface MapComponentProps {
  visibleCategories: Set<string>;
  onMapClick: (lat: number, lng: number) => void;
  temporaryPin: { lat: number; lng: number } | null;
}

export function MapComponent({ visibleCategories, onMapClick, temporaryPin }: MapComponentProps) {
  const { data: entries = [] } = useListEntries();
  const queryClient = useQueryClient();
  const deleteEntryMutation = useDeleteEntry();

  const [selectedEntry, setSelectedEntry] = useState<Entry | null>(null);

  const filteredEntries = useMemo(() => {
    return entries.filter((e) => visibleCategories.has(e.category));
  }, [entries, visibleCategories]);

  const handleDelete = (id: number) => {
    deleteEntryMutation.mutate({ id }, {
      onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: getListEntriesQueryKey() });
        queryClient.invalidateQueries({ queryKey: getGetEntryStatsQueryKey() });
        if (selectedEntry?.id === id) {
          setSelectedEntry(null);
        }
      }
    });
  };

  const handleMapClick = useCallback((e: any) => {
    if (e.detail?.latLng) {
      onMapClick(e.detail.latLng.lat, e.detail.latLng.lng);
      setSelectedEntry(null);
    }
  }, [onMapClick]);

  return (
    <>
      <Map
        defaultZoom={13}
        defaultCenter={{ lat: 37.7749, lng: -122.4194 }}
        mapId="community_tree_map"
        onClick={handleMapClick}
        disableDefaultUI={true}
        className="w-full h-full"
      >
        {filteredEntries.map((entry) => (
          <AdvancedMarker
            key={entry.id}
            position={{ lat: entry.lat, lng: entry.lng }}
            onClick={() => setSelectedEntry(entry)}
          >
            <div
              className="w-8 h-8 rounded-full border-2 border-white shadow-md flex items-center justify-center text-white"
              style={{ backgroundColor: CATEGORY_COLORS[entry.category] }}
            >
              {CATEGORY_ICONS[entry.category]}
            </div>
          </AdvancedMarker>
        ))}

        {temporaryPin && (
          <AdvancedMarker position={temporaryPin}>
            <div className="w-10 h-10 rounded-full border-2 border-white bg-primary shadow-lg flex items-center justify-center text-white animate-bounce">
              <Plus className="w-5 h-5" />
            </div>
          </AdvancedMarker>
        )}

        {selectedEntry && (
          <InfoWindow
            position={{ lat: selectedEntry.lat, lng: selectedEntry.lng }}
            onCloseClick={() => setSelectedEntry(null)}
          >
            <div className="p-2 min-w-[200px] font-sans text-foreground">
              <div className="flex items-center gap-2 mb-2 font-medium">
                <div
                  className="w-6 h-6 rounded-full flex items-center justify-center text-white text-xs"
                  style={{ backgroundColor: CATEGORY_COLORS[selectedEntry.category] }}
                >
                  {CATEGORY_ICONS[selectedEntry.category]}
                </div>
                {CATEGORY_LABELS[selectedEntry.category]}
              </div>
              
              {selectedEntry.notes && (
                <p className="text-sm text-muted-foreground mb-3">{selectedEntry.notes}</p>
              )}
              
              <div className="text-xs text-muted-foreground space-y-1 mb-3">
                <p>Reported by: <span className="font-medium text-foreground">{selectedEntry.reporter}</span></p>
                <p>Date: {format(new Date(selectedEntry.date), 'MMM d, yyyy')}</p>
              </div>

              <button
                onClick={() => handleDelete(selectedEntry.id)}
                disabled={deleteEntryMutation.isPending}
                className="w-full text-xs text-destructive hover:bg-destructive/10 py-1.5 rounded-md transition-colors disabled:opacity-50"
              >
                {deleteEntryMutation.isPending ? 'Deleting...' : 'Delete entry'}
              </button>
            </div>
          </InfoWindow>
        )}
      </Map>
    </>
  );
}
