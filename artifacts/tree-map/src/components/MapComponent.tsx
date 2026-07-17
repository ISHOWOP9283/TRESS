import React, { useState, useCallback, useMemo, useEffect } from 'react';
import {
  useListEntries,
  useDeleteEntry,
  getListEntriesQueryKey,
  getGetEntryStatsQueryKey,
} from '@workspace/api-client-react';
import { useQueryClient } from '@tanstack/react-query';
import { Map, AdvancedMarker, InfoWindow } from '@vis.gl/react-google-maps';
import { EntryCategory, type Entry } from '@workspace/api-client-react';
import { TreePine, Droplets, Trash2 } from 'lucide-react';
import { format } from 'date-fns';
import { toast } from 'sonner';
import type { LatLng } from '@/pages/Home';

export const CATEGORY_COLORS: Record<string, string> = {
  [EntryCategory.tree_planted]: '#2f7d3a',
  [EntryCategory.mangrove_dying]: '#8b6914',
  [EntryCategory.trash_pile]: '#c0392b',
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
  temporaryPin: LatLng | null;
  selectedEntryId: number | null;
  onEntrySelect: (id: number) => void;
  mapRef: React.MutableRefObject<google.maps.Map | null>;
  defaultCenter: LatLng;
  defaultZoom: number;
}

export function MapComponent({
  visibleCategories,
  onMapClick,
  temporaryPin,
  selectedEntryId,
  onEntrySelect,
  mapRef,
  defaultCenter,
  defaultZoom,
}: MapComponentProps) {
  const { data: entries = [] } = useListEntries();
  const queryClient = useQueryClient();
  const deleteEntryMutation = useDeleteEntry();

  const [infoWindowEntry, setInfoWindowEntry] = useState<Entry | null>(null);

  // Sync info window with selected entry
  useEffect(() => {
    if (selectedEntryId === null) {
      setInfoWindowEntry(null);
      return;
    }
    const entry = entries.find((e) => e.id === selectedEntryId) ?? null;
    setInfoWindowEntry(entry);
  }, [selectedEntryId, entries]);

  const filteredEntries = useMemo(
    () => entries.filter((e) => visibleCategories.has(e.category)),
    [entries, visibleCategories]
  );

  const handleDelete = (id: number) => {
    const entry = entries.find((e) => e.id === id);
    deleteEntryMutation.mutate(
      { id },
      {
        onSuccess: () => {
          queryClient.invalidateQueries({ queryKey: getListEntriesQueryKey() });
          queryClient.invalidateQueries({ queryKey: getGetEntryStatsQueryKey() });
          setInfoWindowEntry(null);
          toast.success(`${CATEGORY_LABELS[entry?.category ?? '']} removed`);
        },
        onError: () => {
          toast.error('Failed to delete entry. Please try again.');
        },
      }
    );
  };

  const handleMapClick = useCallback(
    (e: any) => {
      if (e.detail?.latLng) {
        onMapClick(e.detail.latLng.lat, e.detail.latLng.lng);
        setInfoWindowEntry(null);
      }
    },
    [onMapClick]
  );

  const handleMarkerClick = useCallback(
    (entry: Entry) => {
      setInfoWindowEntry(entry);
      onEntrySelect(entry.id);
    },
    [onEntrySelect]
  );

  return (
    <Map
      defaultZoom={defaultZoom}
      defaultCenter={defaultCenter}
      mapId="community_tree_map"
      onClick={handleMapClick}
      gestureHandling="greedy"
      zoomControl={true}
      mapTypeControl={false}
      streetViewControl={false}
      fullscreenControl={true}
      className="w-full h-full"
      onCameraChanged={() => {}} // keep map reactive
      ref={(map) => {
        if (map) (mapRef as any).current = (map as any).map ?? map;
      }}
    >
      {filteredEntries.map((entry) => {
        const isSelected = entry.id === selectedEntryId;
        return (
          <AdvancedMarker
            key={entry.id}
            position={{ lat: entry.lat, lng: entry.lng }}
            onClick={() => handleMarkerClick(entry)}
            zIndex={isSelected ? 10 : 1}
          >
            <div
              className="flex items-center justify-center text-white shadow-lg transition-transform duration-150"
              style={{
                backgroundColor: CATEGORY_COLORS[entry.category],
                width: isSelected ? 40 : 32,
                height: isSelected ? 40 : 32,
                borderRadius: '50%',
                border: isSelected ? '3px solid white' : '2px solid rgba(255,255,255,0.85)',
                boxShadow: isSelected
                  ? '0 0 0 3px ' + CATEGORY_COLORS[entry.category] + '55, 0 4px 12px rgba(0,0,0,0.3)'
                  : '0 2px 6px rgba(0,0,0,0.25)',
                transform: isSelected ? 'scale(1.1)' : 'scale(1)',
              }}
            >
              {CATEGORY_ICONS[entry.category]}
            </div>
          </AdvancedMarker>
        );
      })}

      {temporaryPin && (
        <AdvancedMarker position={temporaryPin} zIndex={20}>
          <div
            className="flex items-center justify-center text-white"
            style={{
              width: 44,
              height: 44,
              borderRadius: '50%',
              backgroundColor: 'hsl(128 32% 35%)',
              border: '3px solid white',
              boxShadow: '0 0 0 3px hsl(128 32% 35% / 0.35), 0 6px 16px rgba(0,0,0,0.35)',
              animation: 'pulse 1.5s ease-in-out infinite',
            }}
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
          </div>
        </AdvancedMarker>
      )}

      {infoWindowEntry && (
        <InfoWindow
          position={{ lat: infoWindowEntry.lat, lng: infoWindowEntry.lng }}
          onCloseClick={() => setInfoWindowEntry(null)}
          pixelOffset={[0, -20]}
        >
          <div className="p-1 min-w-[200px] max-w-[260px] font-sans">
            <div className="flex items-center gap-2 mb-2">
              <div
                className="w-7 h-7 rounded-full flex items-center justify-center text-white flex-shrink-0"
                style={{ backgroundColor: CATEGORY_COLORS[infoWindowEntry.category] }}
              >
                {CATEGORY_ICONS[infoWindowEntry.category]}
              </div>
              <span className="font-semibold text-sm text-gray-900">
                {CATEGORY_LABELS[infoWindowEntry.category]}
              </span>
            </div>

            {infoWindowEntry.notes && (
              <p className="text-sm text-gray-600 mb-2 leading-snug">"{infoWindowEntry.notes}"</p>
            )}

            <div className="text-xs text-gray-500 space-y-0.5 mb-3 border-t border-gray-100 pt-2">
              <p>
                By <span className="font-medium text-gray-700">{infoWindowEntry.reporter}</span>
              </p>
              <p>{format(new Date(infoWindowEntry.date), 'MMM d, yyyy')}</p>
              <p className="font-mono text-[10px] text-gray-400">
                {infoWindowEntry.lat.toFixed(5)}, {infoWindowEntry.lng.toFixed(5)}
              </p>
            </div>

            <button
              onClick={() => handleDelete(infoWindowEntry.id)}
              disabled={deleteEntryMutation.isPending}
              className="w-full text-xs text-red-600 hover:bg-red-50 py-1.5 rounded transition-colors disabled:opacity-50 border border-red-100"
            >
              {deleteEntryMutation.isPending ? 'Removing…' : '🗑 Delete this entry'}
            </button>
          </div>
        </InfoWindow>
      )}
    </Map>
  );
}
