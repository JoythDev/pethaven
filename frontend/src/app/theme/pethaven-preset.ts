==mport { definePreset } from '@primeng/themes';
import Aura from '@primeng/themes/aura';

export const PetHavenPreset = definePreset(Aura, {
  primitive: {
    borderRadius: {
      none: '0',
      xs: '4px',
      sm: '8px',
      md: '12px',
      lg: '16px',
      xl: '24px',
    },
    // success family (sage)
    green: {
      50: '#fafbf9',
      100: '#e0e7da',
      200: '#c4d1b9',
      300: '#b1c3a2',
      400: '#a4b993',
      500: '#9db38a',
      600: '#86a16e',
      700: '#6e8758',
      800: '#566944',
      900: '#3e4c31',
      950: '#262e1e',
    },
    // warn family (honey) — button/tag severity tokens use `orange`
    orange: {
      50: '#fefbf6',
      100: '#f8e6c6',
      200: '#f1d093',
      300: '#edc171',
      400: '#eab659',
      500: '#e8b04b',
      600: '#e39f24',
      700: '#bf8418',
      800: '#966713',
      900: '#6c4a0e',
      950: '#422e08',
    },
    // warn family (honey) — message/toast severity tokens use `yellow`
    yellow: {
      50: '#fefbf6',
      100: '#f8e6c6',
      200: '#f1d093',
      300: '#edc171',
      400: '#eab659',
      500: '#e8b04b',
      600: '#e39f24',
      700: '#bf8418',
      800: '#966713',
      900: '#6c4a0e',
      950: '#422e08',
    },
    // danger family (terracotta)
    red: {
      50: '#fdf9f7',
      100: '#eed2c7',
      200: '#dfa993',
      300: '#d48d70',
      400: '#cd7a58',
      500: '#c96f4a',
      600: '#b65b36',
      700: '#964b2d',
      800: '#763b23',
      900: '#562b1a',
      950: '#361b10',
    },
    // info family (muted teal) — button/tag severity tokens use `sky`
    sky: {
      50: '#f9fafb',
      100: '#cddbdf',
      200: '#9eb8c1',
      300: '#7fa1ad',
      400: '#69919f',
      500: '#5f8794',
      600: '#52747f',
      700: '#446069',
      800: '#354c53',
      900: '#27383d',
      950: '#192427',
    },
    // info family (muted teal) — message/toast severity tokens use `blue`
    blue: {
      50: '#f9fafb',
      100: '#cddbdf',
      200: '#9eb8c1',
      300: '#7fa1ad',
      400: '#69919f',
      500: '#5f8794',
      600: '#52747f',
      700: '#446069',
      800: '#354c53',
      900: '#27383d',
      950: '#192427',
    },
  },
  semantic: {
    // brand olive scale
    primary: {
      50: '#fbfbf9',
      100: '#e2e0d2',
      200: '#c6c4a8',
      300: '#b4b18c',
      400: '#a7a478',
      500: '#a09c6d',
      600: '#8c885b',
      700: '#73704b',
      800: '#5b583b',
      900: '#42402b',
      950: '#2a291b',
    },
    focusRing: {
      width: '2px',
      style: 'solid',
      color: '{primary.700}',
      offset: '2px',
    },
    formField: {
      paddingX: '0.875rem',
      paddingY: '0.625rem',
    },
    colorScheme: {
      light: {
        primary: {
          color: '{primary.700}',
          contrastColor: '#ffffff',
          hoverColor: '{primary.800}',
          activeColor: '{primary.900}',
        },
        // warm neutrals — brand cream (#f2ece0) is the page surface
        surface: {
          0: '#ffffff',
          50: '#f8f5ee',
          100: '#f2ece0',
          200: '#e2dbcf',
          300: '#d5cfc3',
          400: '#b9b1a6',
          500: '#989084',
          600: '#7c7267',
          700: '#63594e',
          800: '#4b4035',
          900: '#382d23',
          950: '#2d2117',
        },
        text: {
          color: '#261a10',
          mutedColor: '#6b6156',
        },
      },
    },
  },
  components: {
    button: {
      root: {
        borderRadius: '9999px',
        label: {
          fontWeight: '700',
        },
      },
      colorScheme: {
        light: {
          root: {
            secondary: {
              color: '{surface.700}',
              hoverColor: '{surface.800}',
              activeColor: '{surface.900}',
            },
            success: {
              background: '{green.800}',
              hoverBackground: '{green.900}',
              activeBackground: '{green.950}',
              borderColor: '{green.800}',
              hoverBorderColor: '{green.900}',
              activeBorderColor: '{green.950}',
            },
            warn: {
              background: '{orange.500}',
              hoverBackground: '{orange.600}',
              activeBackground: '{orange.700}',
              borderColor: '{orange.500}',
              hoverBorderColor: '{orange.600}',
              activeBorderColor: '{orange.700}',
              color: '#261a10',
              hoverColor: '#261a10',
              activeColor: '#261a10',
            },
            danger: {
              background: '{red.700}',
              hoverBackground: '{red.800}',
              activeBackground: '{red.900}',
              borderColor: '{red.700}',
              hoverBorderColor: '{red.800}',
              activeBorderColor: '{red.900}',
            },
            info: {
              background: '{sky.700}',
              hoverBackground: '{sky.800}',
              activeBackground: '{sky.900}',
              borderColor: '{sky.700}',
              hoverBorderColor: '{sky.800}',
              activeBorderColor: '{sky.900}',
            },
          },
          outlined: {
            secondary: { color: '{surface.600}' },
            success: { color: '{green.800}' },
            warn: { color: '{orange.800}' },
            danger: { color: '{red.700}' },
            info: { color: '{sky.700}' },
          },
          text: {
            secondary: { color: '{surface.600}' },
            success: { color: '{green.800}' },
            warn: { color: '{orange.800}' },
            danger: { color: '{red.700}' },
            info: { color: '{sky.700}' },
          },
        },
      },
    },
    tag: {
      colorScheme: {
        light: {
          primary: { color: '{primary.800}' },
          success: { color: '{green.800}' },
          warn: { color: '{orange.900}' },
          danger: { color: '{red.800}' },
        },
      },
    },
    message: {
      colorScheme: {
        light: {
          success: {
            color: '{green.800}',
            outlined: { color: '{green.800}' },
            simple: { color: '{green.800}' },
          },
          warn: {
            color: '{yellow.800}',
            outlined: { color: '{yellow.800}' },
            simple: { color: '{yellow.800}' },
          },
          error: {
            color: '{red.700}',
            outlined: { color: '{red.700}' },
            simple: { color: '{red.700}' },
          },
          secondary: {
            color: '{surface.700}',
            outlined: { color: '{surface.700}' },
            simple: { color: '{surface.700}' },
          },
        },
      },
    },
    toast: {
      colorScheme: {
        light: {
          success: { color: '{green.800}' },
          warn: { color: '{yellow.800}' },
          error: { color: '{red.700}' },
          secondary: { color: '{surface.700}' },
        },
      },
    },
  },
});
