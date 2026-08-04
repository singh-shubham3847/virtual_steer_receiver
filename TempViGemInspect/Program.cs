using System;
using System.Linq;
using System.Reflection;
class Program
{
    static void Main()
    {
        var a = Assembly.LoadFrom(@"C:\Users\Shubham\.nuget\packages\nefarius.vigem.client\1.21.256\lib\netstandard2.0\Nefarius.ViGEm.Client.dll");
        var targetTypes = a.GetTypes().Where(t => t.FullName != null && t.FullName.Contains("Xbox360")).OrderBy(t => t.FullName).ToArray();
        foreach (var t in targetTypes)
        {
            Console.WriteLine(t.FullName + " (" + t.Attributes + ")");
            Console.WriteLine("  Public nested types:");
            foreach (var nt in t.GetNestedTypes(BindingFlags.Public | BindingFlags.Static | BindingFlags.Instance | BindingFlags.NonPublic))
            {
                Console.WriteLine("    " + nt.FullName + " (" + nt.Attributes + ")");
            }
            Console.WriteLine("  Public fields:");
            foreach (var f in t.GetFields(BindingFlags.Public | BindingFlags.Static | BindingFlags.Instance))
            {
                Console.WriteLine("    " + f.Name + " : " + f.FieldType.FullName + " (" + f.Attributes + ")");
            }
            Console.WriteLine("  Public properties:");
            foreach (var p in t.GetProperties(BindingFlags.Public | BindingFlags.Static | BindingFlags.Instance))
            {
                Console.WriteLine("    " + p.Name + " : " + p.PropertyType.FullName);
            }
            Console.WriteLine("  Public methods:");
            foreach (var m in t.GetMethods(BindingFlags.Public | BindingFlags.Static | BindingFlags.Instance | BindingFlags.DeclaredOnly).OrderBy(m => m.Name))
            {
                Console.WriteLine("    " + m.Name + "(" + string.Join(", ", m.GetParameters().Select(p => p.ParameterType.Name + " " + p.Name)) + ")");
            }
            Console.WriteLine();
        }
    }
}
