import React, { useState, useCallback, useRef } from 'react';
import { APIProvider, useMap } from '@vis.gl/react-google-maps';
import { LeftSidebar } from '@/components/LeftSidebar';
import { MapComponent } from '@/components/MapComponent';
import { MapSearch } from '@/components/MapSearch';
import { AddEntryForm } from '@/components/AddEntryForm';
import { EntryCategory } from '@workspace/api-client-react';

declare const __GOOGLE_MAPS_API_KEY__: string;
const API_KEY = __GOOGLE_MAPS_API_KEY__;

// Component to handle map interactions like panning
function MapController({ center }: { center: { lat: number; lng: number } | null }) {
  const map = useMap();
  
  React.useEffect(() => {
    if (map && center) {
      map.panTo(center);
      map.setZoom(16);
    }
  }, [map, center]);

  return null;
}

export default function Home() {
  const [visibleCategories, setVisibleCategories] = useState<Set<string>>(
    new Set(Object.values(EntryCategory))
  );
  
  const [temporaryPin, setTemporaryPin] = useState<{ lat: number; lng: number } | null>(null);
  const [mapCenter, setMapCenter] = useState<{ lat: number; lng: number } | null>(null);

  const handleMapClick = useCallback((lat: number, lng: number) => {
    setTemporaryPin({ lat, lng });
    setMapCenter({ lat, lng });
  }, []);

  const handleSidebarEntrySelect = useCallback((lat: number, lng: number) => {
    setMapCenter({ lat, lng });
    setTemporaryPin(null); // Clear temporary pin if selecting an existing entry
  }, []);

  const handlePlaceSelect = useCallback((place: google.maps.places.PlaceResult) => {
    if (place.geometry?.location) {
      const lat = place.geometry.location.lat();
      const lng = place.geometry.location.lng();
      setMapCenter({ lat, lng });
    }
  }, []);

  if (!API_KEY) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background text-foreground">
        <div className="text-center p-8 max-w-md">
          <h1 className="text-2xl font-serif font-bold text-destructive mb-2">Configuration Error</h1>
          <p className="text-muted-foreground">Google Maps API key is missing. Please set VITE_GOOGLE_MAPS_API_KEY in your environment.</p>
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
          onSelectEntry={handleSidebarEntrySelect}
        />
        
        <div className="flex-1 relative">
          <MapComponent 
            visibleCategories={visibleCategories}
            onMapClick={handleMapClick}
            temporaryPin={temporaryPin}
          />
          <MapController center={mapCenter} />
          
          <div className="absolute top-6 left-1/2 -translate-x-1/2 z-10">
            <MapSearch onPlaceSelect={handlePlaceSelect} />
          </div>

          {temporaryPin && (
            <div className="absolute top-1/2 left-1/2 -translate-y-1/2 ml-4 z-20 pointer-events-auto">
              <AddEntryForm 
                lat={temporaryPin.lat}
                lng={temporaryPin.lng}
                onClose={() => setTemporaryPin(null)}
                onSuccess={() => setTemporaryPin(null)}
              />
            </div>
          )}
        </div>
      </APIProvider>
    </div>
  );
}
