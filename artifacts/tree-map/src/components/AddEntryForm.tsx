import React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { useCreateEntry, getListEntriesQueryKey, getGetEntryStatsQueryKey, EntryInputCategory } from '@workspace/api-client-react';
import { useQueryClient } from '@tanstack/react-query';
import { Card, CardContent, CardHeader, CardTitle, CardDescription, CardFooter } from '@/components/ui/card';
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Button } from '@/components/ui/button';
import { X, Loader2 } from 'lucide-react';
import { CATEGORY_LABELS } from './MapComponent';

const formSchema = z.object({
  category: z.nativeEnum(EntryInputCategory, {
    required_error: "Please select a category.",
  }),
  reporter: z.string().min(1, "Reporter name is required").max(100),
  notes: z.string().optional(),
});

type FormValues = z.infer<typeof formSchema>;

interface AddEntryFormProps {
  lat: number;
  lng: number;
  onClose: () => void;
  onSuccess: () => void;
}

export function AddEntryForm({ lat, lng, onClose, onSuccess }: AddEntryFormProps) {
  const queryClient = useQueryClient();
  const createEntry = useCreateEntry();

  const form = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      reporter: '',
      notes: '',
    },
  });

  const onSubmit = (values: FormValues) => {
    createEntry.mutate({
      data: {
        lat,
        lng,
        category: values.category,
        reporter: values.reporter,
        notes: values.notes,
      }
    }, {
      onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: getListEntriesQueryKey() });
        queryClient.invalidateQueries({ queryKey: getGetEntryStatsQueryKey() });
        onSuccess();
      }
    });
  };

  return (
    <Card className="w-80 shadow-2xl border-border animate-in fade-in zoom-in-95 duration-200">
      <CardHeader className="pb-4 relative">
        <Button 
          variant="ghost" 
          size="icon" 
          className="absolute right-2 top-2 h-8 w-8 text-muted-foreground hover:text-foreground"
          onClick={onClose}
        >
          <X className="w-4 h-4" />
        </Button>
        <CardTitle className="font-serif">Add Log Entry</CardTitle>
        <CardDescription>
          Record a new observation at this location.
        </CardDescription>
      </CardHeader>
      
      <CardContent>
        <Form {...form}>
          <form id="add-entry-form" onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="category"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Category</FormLabel>
                  <Select onValueChange={field.onChange} defaultValue={field.value}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Select type of entry..." />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {Object.entries(CATEGORY_LABELS).map(([value, label]) => (
                        <SelectItem key={value} value={value}>
                          {label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="reporter"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Your Name</FormLabel>
                  <FormControl>
                    <Input placeholder="Jane Doe" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="notes"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Field Notes</FormLabel>
                  <FormControl>
                    <Textarea 
                      placeholder="Describe what you see..." 
                      className="resize-none h-24"
                      {...field} 
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </form>
        </Form>
      </CardContent>
      
      <CardFooter className="flex justify-end gap-2 pt-2">
        <Button variant="outline" onClick={onClose} type="button">
          Cancel
        </Button>
        <Button 
          type="submit" 
          form="add-entry-form" 
          disabled={createEntry.isPending}
        >
          {createEntry.isPending && <Loader2 className="w-4 h-4 mr-2 animate-spin" />}
          Save Entry
        </Button>
      </CardFooter>
    </Card>
  );
}
