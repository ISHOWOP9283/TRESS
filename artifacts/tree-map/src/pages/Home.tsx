import React, { useState, useCallback, useRef, useEffect } from 'react';
import { APIProvider } from '@vis.gl/react-google-maps';
import { LeftSidebar } from '@/components/LeftSidebar';
import { MapComponent } from '@/components/MapComponent';
import { MapSearch } from '@/components/MapSearch';
import { AddEntryForm } from '@/components/AddEntryForm';
import { Toaster } from 'sonner';
import { MapPin } from 'lucide-react';

declare const __GOOGLE_MAPS_API_KEY__: string;
const API_KEY = __GOOGLE_MAPS_API_KEY__;

export type LatLng = { lat: number; lng: number };

// Default center: world view — will be overridden by geolocation
const WORLD_CENTER: LatLng = { lat: 20, lng: 0 };
const WORLD_ZOOM = 2;

export default function Home() {
  const [visibleCategories, setVisibleCategories] = useState<Set<string>>(
    new Set(['tree_planted', 'mangrove_dying', 'trash_pile'])
  );
  const [temporaryPin, setTemporaryPin] = useState<LatLng | null>(null);
  const [selectedEntryId, setSelectedEntryId] = useState<number | null>(null);

  // Map imperative control
  const mapRef = useRef<google.maps.Map | null>(null);

  // Geolocation on mount
  useEffect(() => {
    if (!navigator.geolocation) return;
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        mapRef.current?.setCenter({ lat: pos.coords.latitude, lng: pos.coords.longitude });
        mapRef.current?.setZoom(13);
      },
      () => {
        // Fallback — keep world view
      },
      { timeout: 8000 }
    );
  }, []);

  const handleMapClick = useCallback((lat: number, lng: number) => {
    setTemporaryPin({ lat, lng });
    setSelectedEntryId(null);
  }, []);

  const handleSidebarEntrySelect = useCallback((id: number, lat: number, lng: number) => {
    setSelectedEntryId(id);
    setTemporaryPin(null);
    mapRef.current?.panTo({ lat, lng });
    const zoom = mapRef.current?.getZoom() ?? 0;
    if (zoom < 14) mapRef.current?.setZoom(14);
  }, []);

  const handleMapEntrySelect = useCallback((id: number) => {
    setSelectedEntryId(id);
    setTemporaryPin(null);
  }, []);

  const handlePlaceSelect = useCallback((place: google.maps.places.PlaceResult) => {
    if (place.geometry?.location) {
      const lat = place.geometry.location.lat();
      const lng = place.geometry.location.lng();
      mapRef.current?.panTo({ lat, lng });
      mapRef.current?.setZoom(15);
    }
  }, []);

  const handleFormClose = useCallback(() => {
    setTemporaryPin(null);
  }, []);

  const handleFormSuccess = useCallback(() => {
    setTemporaryPin(null);
  }, []);

  if (!API_KEY) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background text-foreground">
        <div className="text-center p-8 max-w-md">
          <h1 className="text-2xl font-serif font-bold text-destructive mb-2">Configuration Error</h1>
          <p className="text-muted-foreground">Google Maps API key is missing. Please set GOOGLE_MAPS_API_KEY as a secret.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="w-screen h-[100dvh] flex relative overflow-hidden bg-background">
      <APIProvider apiKey={API_KEY}>
        <LeftSidebar
          visibleCategories={visibleCategories}
          setVisibleCategories={setVisibleCategories}
          selectedEntryId={selectedEntryId}
          onSelectEntry={handleSidebarEntrySelect}
        />

        <div className="flex-1 relative">
          <MapComponent
            visibleCategories={visibleCategories}
            onMapClick={handleMapClick}
            temporaryPin={temporaryPin}
            selectedEntryId={selectedEntryId}
            onEntrySelect={handleMapEntrySelect}
            mapRef={mapRef}
            defaultCenter={WORLD_CENTER}
            defaultZoom={WORLD_ZOOM}
          />

          {/* Search bar */}
          <div className="absolute top-4 left-1/2 -translate-x-1/2 z-10 w-[min(420px,calc(100vw-2rem))]">
            <MapSearch onPlaceSelect={handlePlaceSelect} mapRef={mapRef} />
          </div>

          {/* Tap-to-log hint */}
          {!temporaryPin && (
            <div className="absolute bottom-6 left-1/2 -translate-x-1/2 z-10 pointer-events-none">
              <div className="flex items-center gap-2 px-4 py-2 rounded-full bg-background/80 backdrop-blur-sm border border-border shadow-md text-sm text-muted-foreground">
                <MapPin className="w-4 h-4 text-primary" />
                Tap anywhere on the map to log an observation
              </div>
            </div>
          )}

          {/* Add entry form — fixed bottom-right */}
          {temporaryPin && (
            <div className="absolute bottom-6 right-6 z-20">
              <AddEntryForm
                lat={temporaryPin.lat}
                lng={temporaryPin.lng}
                onClose={handleFormClose}
                onSuccess={handleFormSuccess}
              />
            </div>
          )}
        </div>

        <Toaster position="top-right" richColors />
      </APIProvider>
    </div>
  );
}
